package fun.ai.studio.deploy.runtime.interfaces.dto;

import fun.ai.studio.deploy.runtime.domain.RuntimeNode;

import java.time.Instant;

public class RuntimeNodeResponse {
    private Long nodeId;
    private String name;
    private String agentBaseUrl;
    private String gatewayBaseUrl;
    private Boolean enabled;
    private Integer weight;
    private Instant lastHeartbeatAt;
    private String health;

    public static RuntimeNodeResponse from(RuntimeNode n, String health) {
        RuntimeNodeResponse r = new RuntimeNodeResponse();
        r.setNodeId(n == null || n.getId() == null ? null : n.getId().value());
        r.setName(n == null ? null : n.getName());
        r.setAgentBaseUrl(n == null ? null : n.getAgentBaseUrl());
        r.setGatewayBaseUrl(n == null ? null : n.getGatewayBaseUrl());
        r.setEnabled(n != null && n.isEnabled());
        r.setWeight(n == null ? null : n.getWeight());
        r.setLastHeartbeatAt(n == null ? null : n.getLastHeartbeatAt());
        r.setHealth(health);
        return r;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(Instant lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public String getHealth() {
        return health;
    }

    public void setHealth(String health) {
        this.health = health;
    }
}


