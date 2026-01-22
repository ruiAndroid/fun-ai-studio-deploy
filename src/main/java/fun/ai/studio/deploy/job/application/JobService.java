package fun.ai.studio.deploy.job.application;

import fun.ai.studio.common.NotFoundException;
import fun.ai.studio.common.ConflictException;
import fun.ai.studio.deploy.job.domain.Job;
import fun.ai.studio.deploy.job.domain.JobId;
import fun.ai.studio.deploy.job.domain.JobStatus;
import fun.ai.studio.deploy.job.domain.JobType;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Job 用例服务：只编排领域对象与仓储，不做任何基础设施细节。
 */
@Service
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public Job create(JobType type, Map<String, Object> payload) {
        // 规则：同一 appId 的部署互斥（进行中 PENDING/RUNNING 不允许再创建）
        String appId = extractAppId(payload);
        if (appId != null && jobRepository.existsActiveJobForApp(appId)) {
            throw new ConflictException("该应用正在部署中（appId=" + appId + "），请等待完成或先取消后再试");
        }
        Job job = Job.create(type, payload);
        return jobRepository.save(job);
    }

    public Job get(String jobId) {
        return jobRepository.findById(new JobId(jobId))
                .orElseThrow(() -> new NotFoundException("Job 不存在：" + jobId));
    }

    public List<Job> list(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return jobRepository.list(safeLimit);
    }

    public Optional<Job> claimNext(String runnerId, Duration leaseDuration) {
        return jobRepository.claimNext(runnerId, leaseDuration);
    }

    public Job heartbeat(String jobId, String runnerId, Duration extendDuration) {
        if (extendDuration == null || extendDuration.isZero() || extendDuration.isNegative()) {
            throw new IllegalArgumentException("extendDuration 必须为正数");
        }
        Job job = get(jobId);
        Instant newLeaseExpireAt = Instant.now().plus(extendDuration);
        Job updated = job.heartbeat(runnerId, newLeaseExpireAt);
        return jobRepository.save(updated);
    }

    public Job report(String jobId, String runnerId, JobStatus to, String errorMessage) {
        Job job = get(jobId);
        // 权限校验：仅当前 runner 可上报（RUNNING 时）
        if (job.getStatus() == JobStatus.RUNNING && job.getRunnerId() != null && !job.getRunnerId().equals(runnerId)) {
            throw new fun.ai.studio.common.ConflictException("runnerId 不匹配，禁止上报");
        }
        return transition(jobId, to, errorMessage);
    }

    public Job transition(String jobId, JobStatus to, String errorMessage) {
        Job job = get(jobId);

        Job updated;
        switch (to) {
            case RUNNING -> {
                throw new IllegalArgumentException("RUNNING 只能通过 claimNext 进入");
            }
            case SUCCEEDED -> updated = job.succeed();
            case FAILED -> updated = job.fail(errorMessage);
            case CANCELLED -> updated = job.cancel();
            case PENDING -> updated = job.transitionTo(JobStatus.PENDING, null);
            default -> updated = job.transitionTo(to, errorMessage);
        }

        return jobRepository.save(updated);
    }

    public Job cancel(String jobId) {
        return transition(jobId, JobStatus.CANCELLED, null);
    }

    private static String extractAppId(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return null;
        Object v = payload.get("appId");
        if (v == null) return null;
        String s = String.valueOf(v);
        return (s == null || s.isBlank()) ? null : s;
    }
}


