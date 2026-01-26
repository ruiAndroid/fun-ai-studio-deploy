package fun.ai.studio.deploy.job.infrastructure;

import fun.ai.studio.deploy.job.application.JobRepository;
import fun.ai.studio.deploy.job.domain.Job;
import fun.ai.studio.deploy.job.domain.JobId;
import fun.ai.studio.deploy.job.domain.JobStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * InMemory 实现：用于本地开发/单测；后续替换 MySQL/Mongo 不影响应用层/接口层。
 */
public class InMemoryJobRepository implements JobRepository {

    private final ConcurrentMap<String, Job> store = new ConcurrentHashMap<>();
    private final Object claimLock = new Object();

    @Override
    public Job save(Job job) {
        store.put(job.getId().value(), job);
        return job;
    }

    @Override
    public Optional<Job> findById(JobId id) {
        return Optional.ofNullable(store.get(id.value()));
    }

    @Override
    public List<Job> list(int limit) {
        return store.values().stream()
                .sorted(Comparator.comparing(Job::getCreatedAt).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public boolean existsActiveJobForApp(String appId) {
        if (appId == null || appId.isBlank()) return false;
        return store.values().stream()
                .anyMatch(j -> {
                    if (!appId.equals(String.valueOf(j.getPayload() == null ? null : j.getPayload().get("appId")))) return false;
                    if (j.getStatus() == JobStatus.PENDING) return true;
                    if (j.getStatus() == JobStatus.RUNNING) {
                        Instant lease = j.getLeaseExpireAt();
                        return lease != null && lease.isAfter(Instant.now());
                    }
                    return false;
                });
    }

    @Override
    public Optional<Job> claimNext(String runnerId, Duration leaseDuration) {
        if (leaseDuration == null || leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("leaseDuration 必须为正数");
        }
        Instant leaseExpireAt = Instant.now().plus(leaseDuration);

        synchronized (claimLock) {
            // 选择最早创建的 PENDING job（FIFO），保证公平
            Optional<Job> candidate = store.values().stream()
                    .filter(j -> j.getStatus() == JobStatus.PENDING)
                    .sorted(Comparator.comparing(Job::getCreatedAt))
                    .findFirst();

            if (candidate.isEmpty()) {
                // 尝试回收过期 RUNNING
                Optional<Job> expiredRunning = store.values().stream()
                        .filter(j -> j.getStatus() == JobStatus.RUNNING)
                        .filter(j -> j.getLeaseExpireAt() == null || j.getLeaseExpireAt().isBefore(Instant.now()))
                        .sorted(Comparator.comparing(Job::getCreatedAt))
                        .findFirst();
                if (expiredRunning.isEmpty()) return Optional.empty();
                Job reclaimed = expiredRunning.get().reclaimByLeaseTimeout();
                store.put(reclaimed.getId().value(), reclaimed);
                Job claimed = reclaimed.claim(runnerId, leaseExpireAt);
                store.put(claimed.getId().value(), claimed);
                return Optional.of(claimed);
            }

            Job pending = candidate.get();
            Job claimed = pending.claim(runnerId, leaseExpireAt);
            store.put(claimed.getId().value(), claimed);
            return Optional.of(claimed);
        }
    }

    @Override
    public long deleteByAppId(String appId) {
        if (appId == null || appId.isBlank()) return 0;
        long removed = 0;
        for (String k : store.keySet()) {
            Job j = store.get(k);
            if (j == null) continue;
            Object v = j.getPayload() == null ? null : j.getPayload().get("appId");
            if (v == null) continue;
            if (!appId.equals(String.valueOf(v))) continue;
            if (store.remove(k, j)) removed++;
        }
        return removed;
    }

    @Override
    public List<Job> listByAppId(String appId, int limit) {
        if (appId == null || appId.isBlank()) return List.of();
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return store.values().stream()
                .filter(j -> {
                    Object v = j.getPayload() == null ? null : j.getPayload().get("appId");
                    return v != null && appId.equals(String.valueOf(v));
                })
                .sorted(Comparator.comparing(Job::getCreatedAt).reversed())
                .limit(safeLimit)
                .toList();
    }
}


