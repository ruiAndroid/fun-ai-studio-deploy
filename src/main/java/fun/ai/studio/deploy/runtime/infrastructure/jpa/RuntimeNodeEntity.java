package fun.ai.studio.deploy.runtime.infrastructure.jpa;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Deploy 控制面 runtime 节点表（可持久化）。
 *
 * 对齐 API 侧 workspace-node 的落库风格：
 * - 表名：fun_ai_*
 * - 字段：create_time / update_time / last_heartbeat_at
 */
@Entity
@Table(
        name = "fun_ai_deploy_runtime_node",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fun_ai_deploy_runtime_node_name", columnNames = {"name"})
        }
)
public class RuntimeNodeEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "agent_base_url", nullable = true, length = 512)
    private String agentBaseUrl;

    @Column(name = "gateway_base_url", nullable = true, length = 512)
    private String gatewayBaseUrl;

    /**
     * 0/1（对齐 API MyBatis-Plus 的 enabled 字段习惯）
     */
    @Column(name = "enabled", nullable = false)
    private Integer enabled;

    @Column(name = "weight", nullable = false)
    private Integer weight;

    @Column(name = "last_heartbeat_at", nullable = true)
    private LocalDateTime lastHeartbeatAt;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAgentBaseUrl() {
        return agentBaseUrl;
    }

    public void setAgentBaseUrl(String agentBaseUrl) {
        this.agentBaseUrl = agentBaseUrl;
    }

    public String getGatewayBaseUrl() {
        return gatewayBaseUrl;
    }

    public void setGatewayBaseUrl(String gatewayBaseUrl) {
        this.gatewayBaseUrl = gatewayBaseUrl;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public LocalDateTime getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(LocalDateTime lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
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


