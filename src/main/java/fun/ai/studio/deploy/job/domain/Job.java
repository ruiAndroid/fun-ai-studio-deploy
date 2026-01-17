package fun.ai.studio.deploy.job.domain;

import fun.ai.studio.common.ConflictException;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 领域实体：Job（部署控制面里的“任务”）。
 *
 * 设计原则：
 * - 不依赖 Spring
 * - 状态流转只能通过领域方法完成
 */
public final class Job {
    private final JobId id;
    private final JobType type;
    private final JobStatus status;
    private final Map<String, Object> payload;
    private final String errorMessage;
    /**
     * 当前领取该任务的 runner（仅 RUNNING 时有效）。
     */
    private final String runnerId;
    /**
     * 任务租约到期时间：runner 必须在此之前 heartbeat 续租，否则可被回收/重试（策略由应用层决定）。
     */
    private final Instant leaseExpireAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Job(JobId id,
                JobType type,
                JobStatus status,
                Map<String, Object> payload,
                String errorMessage,
                String runnerId,
                Instant leaseExpireAt,
                Instant createdAt,
                Instant updatedAt) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.payload = payload == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(payload));
        this.errorMessage = errorMessage;
        this.runnerId = runnerId;
        this.leaseExpireAt = leaseExpireAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Job create(JobType type, Map<String, Object> payload) {
        Instant now = Instant.now();
        return new Job(JobId.newId(), type, JobStatus.PENDING, payload, null, null, null, now, now);
    }

    public JobId getId() {
        return id;
    }

    public JobType getType() {
        return type;
    }

    public JobStatus getStatus() {
        return status;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getRunnerId() {
        return runnerId;
    }

    public Instant getLeaseExpireAt() {
        return leaseExpireAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 由 runner 领取任务：PENDING -> RUNNING，并设置 runnerId 与租约到期时间。
     */
    public Job claim(String runnerId, Instant leaseExpireAt) {
        if (runnerId == null || runnerId.isBlank()) {
            throw new IllegalArgumentException("runnerId 不能为空");
        }
        if (leaseExpireAt == null) {
            throw new IllegalArgumentException("leaseExpireAt 不能为空");
        }
        Job claimed = transitionTo(JobStatus.RUNNING, null);
        return new Job(this.id, this.type, claimed.status, this.payload, null, runnerId, leaseExpireAt, this.createdAt, claimed.updatedAt);
    }

    /**
     * runner 心跳续租：仅允许当前 runner 续租。
     */
    public Job heartbeat(String runnerId, Instant newLeaseExpireAt) {
        if (this.status != JobStatus.RUNNING) {
            throw new ConflictException("仅 RUNNING 状态允许 heartbeat");
        }
        if (runnerId == null || runnerId.isBlank()) {
            throw new IllegalArgumentException("runnerId 不能为空");
        }
        if (!runnerId.equals(this.runnerId)) {
            throw new ConflictException("runnerId 不匹配，禁止续租");
        }
        if (newLeaseExpireAt == null) {
            throw new IllegalArgumentException("newLeaseExpireAt 不能为空");
        }
        Instant now = Instant.now();
        return new Job(this.id, this.type, this.status, this.payload, this.errorMessage, this.runnerId, newLeaseExpireAt, this.createdAt, now);
    }

    public Job succeed() {
        Job updated = transitionTo(JobStatus.SUCCEEDED, null);
        return new Job(this.id, this.type, updated.status, this.payload, null, this.runnerId, this.leaseExpireAt, this.createdAt, updated.updatedAt);
    }

    public Job fail(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            errorMessage = "任务失败";
        }
        Job updated = transitionTo(JobStatus.FAILED, errorMessage);
        return new Job(this.id, this.type, updated.status, this.payload, updated.errorMessage, this.runnerId, this.leaseExpireAt, this.createdAt, updated.updatedAt);
    }

    public Job cancel() {
        Job updated = transitionTo(JobStatus.CANCELLED, null);
        return new Job(this.id, this.type, updated.status, this.payload, null, this.runnerId, this.leaseExpireAt, this.createdAt, updated.updatedAt);
    }

    public Job transitionTo(JobStatus to, String errorMessage) {
        if (!JobTransitionRules.canTransition(this.status, to)) {
            throw new ConflictException("非法状态流转：" + this.status + " -> " + to);
        }
        Instant now = Instant.now();
        return new Job(this.id, this.type, to, this.payload, errorMessage, this.runnerId, this.leaseExpireAt, this.createdAt, now);
    }
}


