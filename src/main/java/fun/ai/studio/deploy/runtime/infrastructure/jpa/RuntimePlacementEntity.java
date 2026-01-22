package fun.ai.studio.deploy.runtime.infrastructure.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Deploy 控制面 appId -> runtimeNode 的粘性落点（可持久化）。
 */
@Entity
@Table(name = "fun_ai_deploy_runtime_placement")
public class RuntimePlacementEntity {

    @Id
    @Column(name = "app_id", nullable = false, length = 128)
    private String appId;

    @Column(name = "node_id", nullable = false)
    private Long nodeId;

    @Column(name = "last_active_at", nullable = false)
    private long lastActiveAt;

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

    public long getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(long lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
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


