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

    /**
     * 从持久化记录还原 Job（仅供仓储层使用）。
     *
     * 说明：
     * - 不绕过状态机：外部仍应通过领域方法做状态流转；这里仅用于“读取/重建”。
     */
    public static Job restore(JobId id,
                              JobType type,
                              JobStatus status,
                              Map<String, Object> payload,
                              String errorMessage,
                              String runnerId,
                              Instant leaseExpireAt,
                              Instant createdAt,
                              Instant updatedAt) {
        return new Job(id, type, status, payload, errorMessage, runnerId, leaseExpireAt, createdAt, updatedAt);
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

        // 记录本次 RUNNING 的起始时间（用于全局超时判定）
        Map<String, Object> newPayload = new HashMap<>(this.payload == null ? Collections.emptyMap() : this.payload);
        long nowMs = Instant.now().toEpochMilli();
        newPayload.put("runStartedAt", nowMs);
        // attempt 仅用于展示/排障，不影响逻辑
        try {
            Object old = newPayload.get("attempt");
            long n = 0;
            if (old instanceof Number) n = ((Number) old).longValue();
            else if (old != null) n = Long.parseLong(String.valueOf(old));
            newPayload.put("attempt", n + 1);
        } catch (Exception ignore) {
            newPayload.put("attempt", 1);
        }

        return new Job(this.id, this.type, claimed.status, newPayload, null, runnerId, leaseExpireAt, this.createdAt, claimed.updatedAt);
    }

    /**
     * 回收“过期租约”的 RUNNING 任务：RUNNING -> PENDING，并清空 runnerId/leaseExpireAt。
     * <p>
     * 说明：这是执行面容错所需的“超时回收”机制，不等同于业务层主动 transition。
     */
    public Job reclaimByLeaseTimeout() {
        if (this.status != JobStatus.RUNNING) {
            throw new ConflictException("仅 RUNNING 状态允许回收");
        }
        if (this.leaseExpireAt == null) {
            // 没有 lease 视为可回收
            Instant now = Instant.now();
            return new Job(this.id, this.type, JobStatus.PENDING, this.payload, this.errorMessage, null, null, this.createdAt, now);
        }
        Instant now = Instant.now();
        if (!this.leaseExpireAt.isBefore(now)) {
            throw new ConflictException("lease 未过期，禁止回收");
        }
        return new Job(this.id, this.type, JobStatus.PENDING, this.payload, this.errorMessage, null, null, this.createdAt, now);
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

    /**
     * 更新 payload（用于心跳上报阶段信息等）。
     * <p>
     * 约定：patch 中 value==null 表示删除该 key。
     */
    public Job withPayloadPatch(Map<String, Object> patch) {
        if (patch == null || patch.isEmpty()) return this;
        Map<String, Object> newPayload = new HashMap<>(this.payload == null ? Collections.emptyMap() : this.payload);
        for (Map.Entry<String, Object> e : patch.entrySet()) {
            if (e.getKey() == null) continue;
            if (e.getValue() == null) newPayload.remove(e.getKey());
            else newPayload.put(e.getKey(), e.getValue());
        }
        Instant now = Instant.now();
        return new Job(this.id, this.type, this.status, newPayload, this.errorMessage, this.runnerId, this.leaseExpireAt, this.createdAt, now);
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


