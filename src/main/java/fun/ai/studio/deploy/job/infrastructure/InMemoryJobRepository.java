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
                return Optional.empty();
            }

            Job pending = candidate.get();
            Job claimed = pending.claim(runnerId, leaseExpireAt);
            store.put(claimed.getId().value(), claimed);
            return Optional.of(claimed);
        }
    }
}


