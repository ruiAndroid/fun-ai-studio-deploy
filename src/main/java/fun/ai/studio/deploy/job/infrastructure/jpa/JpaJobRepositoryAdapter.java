package fun.ai.studio.deploy.job.infrastructure.jpa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.ai.studio.deploy.job.application.JobRepository;
import fun.ai.studio.deploy.job.domain.Job;
import fun.ai.studio.deploy.job.domain.JobId;
import fun.ai.studio.deploy.job.domain.JobStatus;
import fun.ai.studio.deploy.job.domain.JobType;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DB 落库实现：使用乐观锁确保 claimNext 不会重复领取。
 */
public class JpaJobRepositoryAdapter implements JobRepository {

    private final JobJpaRepository jpa;
    private final ObjectMapper om;

    public JpaJobRepositoryAdapter(JobJpaRepository jpa, ObjectMapper om) {
        this.jpa = jpa;
        this.om = om;
    }

    @Override
    public Job save(Job job) {
        if (job == null || job.getId() == null || job.getId().value() == null) {
            throw new IllegalArgumentException("job/id 不能为空");
        }
        JobEntity e = jpa.findById(job.getId().value()).orElseGet(JobEntity::new);
        e.setId(job.getId().value());
        e.setType(job.getType() == null ? null : job.getType().name());
        e.setAppId(extractAppId(job.getPayload()));
        e.setStatus(job.getStatus() == null ? null : job.getStatus().name());
        e.setErrorMessage(job.getErrorMessage());
        e.setRunnerId(job.getRunnerId());
        e.setLeaseExpireAt(job.getLeaseExpireAt() == null ? null : job.getLeaseExpireAt().toEpochMilli());
        e.setPayloadJson(writePayload(job.getPayload()));
        // create_time/update_time 由 @PrePersist/@PreUpdate 维护；首次写入可按领域时间回填
        if (e.getCreateTime() == null && job.getCreatedAt() != null) {
            e.setCreateTime(toLocalDateTime(job.getCreatedAt()));
        }
        if (job.getUpdatedAt() != null) {
            e.setUpdateTime(toLocalDateTime(job.getUpdatedAt()));
        }
        JobEntity saved = jpa.save(e);
        return toDomain(saved);
    }

    @Override
    public Optional<Job> findById(JobId id) {
        if (id == null || id.value() == null) return Optional.empty();
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Job> list(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return jpa.findAllByOrderByCreateTimeDesc(PageRequest.of(0, safeLimit))
                .getContent()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsActiveJobForApp(String appId) {
        if (appId == null || appId.isBlank()) return false;
        return jpa.existsByAppIdAndStatusIn(appId, List.of(JobStatus.PENDING.name(), JobStatus.RUNNING.name()));
    }

    @Override
    public Optional<Job> claimNext(String runnerId, Duration leaseDuration) {
        if (leaseDuration == null || leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("leaseDuration 必须为正数");
        }
        Instant leaseExpireAt = Instant.now().plus(leaseDuration);

        // 多 runner 并发：用乐观锁重试，避免重复领取
        for (int i = 0; i < 20; i++) {
            List<JobEntity> candidates = jpa.findByStatusOrderByCreateTimeAsc(JobStatus.PENDING.name(), PageRequest.of(0, 1))
                    .getContent();
            if (candidates.isEmpty()) return Optional.empty();
            JobEntity e = candidates.get(0);

            Job pending = toDomain(e);
            Job claimed = pending.claim(runnerId, leaseExpireAt);
            try {
                save(claimed); // 触发 version 变更；并发冲突会抛 OptimisticLockingFailureException
                return Optional.of(claimed);
            } catch (OptimisticLockingFailureException ignore) {
                // retry
            }
        }
        return Optional.empty();
    }

    private Job toDomain(JobEntity e) {
        if (e == null) return null;
        Map<String, Object> payload = readPayload(e.getPayloadJson());
        Instant createdAt = toInstant(e.getCreateTime());
        Instant updatedAt = toInstant(e.getUpdateTime());
        Instant lease = e.getLeaseExpireAt() == null ? null : Instant.ofEpochMilli(e.getLeaseExpireAt());
        return Job.restore(
                new JobId(e.getId()),
                e.getType() == null ? null : JobType.valueOf(e.getType()),
                e.getStatus() == null ? null : JobStatus.valueOf(e.getStatus()),
                payload,
                e.getErrorMessage(),
                e.getRunnerId(),
                lease,
                createdAt,
                updatedAt
        );
    }

    private String writePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return null;
        try {
            return om.writeValueAsString(payload);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> readPayload(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return om.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private static String extractAppId(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return null;
        Object v = payload.get("appId");
        if (v == null) return null;
        String s = String.valueOf(v);
        return s.isBlank() ? null : s;
    }

    private static LocalDateTime toLocalDateTime(Instant at) {
        if (at == null) return null;
        return LocalDateTime.ofInstant(at, ZoneId.systemDefault());
    }

    private static Instant toInstant(LocalDateTime t) {
        if (t == null) return null;
        return t.atZone(ZoneId.systemDefault()).toInstant();
    }
}


