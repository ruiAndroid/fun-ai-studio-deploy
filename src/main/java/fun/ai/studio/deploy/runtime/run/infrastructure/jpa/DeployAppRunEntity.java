package fun.ai.studio.deploy.runtime.run.infrastructure.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 对齐 API 的 fun_ai_workspace_run：Deploy 的 last-known app run。
 */
@Entity
@Table(name = "fun_ai_deploy_app_run")
public class DeployAppRunEntity {

    @Id
    @Column(name = "app_id", nullable = false, length = 128)
    private String appId;

    @Column(name = "node_id", nullable = true)
    private Long nodeId;

    @Column(name = "last_job_id", nullable = true, length = 64)
    private String lastJobId;

    @Column(name = "last_job_status", nullable = true, length = 32)
    private String lastJobStatus;

    @Column(name = "last_error", nullable = true, length = 1024)
    private String lastError;

    @Column(name = "last_deployed_at", nullable = true)
    private Long lastDeployedAt;

    @Column(name = "last_active_at", nullable = true)
    private Long lastActiveAt;

    @Column(name = "create_time", nullable = true)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = true)
    private LocalDateTime updateTime;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createTime == null) createTime = now;
        if (updateTime == null) updateTime = now;
    }

    @PreUpdate
    public void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public String getLastJobId() {
        return lastJobId;
    }

    public void setLastJobId(String lastJobId) {
        this.lastJobId = lastJobId;
    }

    public String getLastJobStatus() {
        return lastJobStatus;
    }

    public void setLastJobStatus(String lastJobStatus) {
        this.lastJobStatus = lastJobStatus;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Long getLastDeployedAt() {
        return lastDeployedAt;
    }

    public void setLastDeployedAt(Long lastDeployedAt) {
        this.lastDeployedAt = lastDeployedAt;
    }

    public Long getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(Long lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
}


