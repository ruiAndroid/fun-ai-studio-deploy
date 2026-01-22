package fun.ai.studio.deploy.job.infrastructure.jpa;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Deploy Job 队列落库（对齐 fun_ai_* 表风格）。
 */
@Entity
@Table(
        name = "fun_ai_deploy_job",
        indexes = {
                @Index(name = "idx_fun_ai_deploy_job_status_create_time", columnList = "status,create_time"),
                @Index(name = "idx_fun_ai_deploy_job_app_id_status", columnList = "app_id,status,create_time"),
                @Index(name = "idx_fun_ai_deploy_job_runner_id", columnList = "runner_id"),
                @Index(name = "idx_fun_ai_deploy_job_lease_expire_at", columnList = "lease_expire_at")
        }
)
public class JobEntity {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "type", nullable = false, length = 64)
    private String type;

    /**
     * 互斥/查询优化字段：从 payload.appId 冗余出来。
     */
    @Column(name = "app_id", nullable = true, length = 64)
    private String appId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Lob
    @Column(name = "payload_json", nullable = true, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "error_message", nullable = true, length = 1024)
    private String errorMessage;

    @Column(name = "runner_id", nullable = true, length = 128)
    private String runnerId;

    /**
     * epoch ms
     */
    @Column(name = "lease_expire_at", nullable = true)
    private Long leaseExpireAt;

    @Column(name = "create_time", nullable = true)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = true)
    private LocalDateTime updateTime;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createTime == null) createTime = now;
        if (updateTime == null) updateTime = now;
        if (version == null) version = 0L;
    }

    @PreUpdate
    public void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRunnerId() {
        return runnerId;
    }

    public void setRunnerId(String runnerId) {
        this.runnerId = runnerId;
    }

    public Long getLeaseExpireAt() {
        return leaseExpireAt;
    }

    public void setLeaseExpireAt(Long leaseExpireAt) {
        this.leaseExpireAt = leaseExpireAt;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}


