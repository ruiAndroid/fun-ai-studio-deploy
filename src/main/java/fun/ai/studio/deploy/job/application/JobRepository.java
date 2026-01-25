package fun.ai.studio.deploy.job.application;

import fun.ai.studio.deploy.job.domain.Job;
import fun.ai.studio.deploy.job.domain.JobId;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 仓储抽象：应用层依赖接口；基础设施层提供实现（InMemory/MySQL/Mongo...）。
 */
public interface JobRepository {
    Job save(Job job);

    Optional<Job> findById(JobId id);

    List<Job> list(int limit);

    /**
     * 互斥用：检查指定 appId 是否存在“进行中”的 Job（PENDING/RUNNING）。
     */
    boolean existsActiveJobForApp(String appId);

    /**
     * 原子领取一个待执行任务（PENDING -> RUNNING）。
     * - 若无可领取任务，返回 empty
     * - 必须保证同一时刻只有一个 runner 能领取到同一个 job
     */
    Optional<Job> claimNext(String runnerId, Duration leaseDuration);

    /**
     * 清理指定 appId 的历史 Job（用于删除应用后的控制面数据清理）。
     *
     * @return 删除条数（best-effort；InMemory/JPA 语义一致）
     */
    long deleteByAppId(String appId);
}


