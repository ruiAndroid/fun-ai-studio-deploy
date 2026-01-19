package fun.ai.studio.deploy.runtime.domain;

import java.time.Instant;

/**
 * Runtime 节点（数据面）：承载用户应用容器 + 网关 + runtime-agent。
 */
public final class RuntimeNode {
    private final RuntimeNodeId id;
    private final String name;
    private final String agentBaseUrl;
    private final String gatewayBaseUrl;
    private final boolean enabled;
    private final int weight;
    private final Instant lastHeartbeatAt;

    private RuntimeNode(RuntimeNodeId id,
                        String name,
                        String agentBaseUrl,
                        String gatewayBaseUrl,
                        boolean enabled,
                        int weight,
                        Instant lastHeartbeatAt) {
        this.id = id;
        this.name = name;
        this.agentBaseUrl = agentBaseUrl;
        this.gatewayBaseUrl = gatewayBaseUrl;
        this.enabled = enabled;
        this.weight = weight;
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public static RuntimeNode create(RuntimeNodeId id,
                                     String name,
                                     String agentBaseUrl,
                                     String gatewayBaseUrl,
                                     boolean enabled,
                                     int weight) {
        return new RuntimeNode(id, name, agentBaseUrl, gatewayBaseUrl, enabled, weight, null);
    }

    public RuntimeNode heartbeat(Instant at, String agentBaseUrl, String gatewayBaseUrl) {
        return new RuntimeNode(
                this.id,
                this.name,
                agentBaseUrl == null ? this.agentBaseUrl : agentBaseUrl,
                gatewayBaseUrl == null ? this.gatewayBaseUrl : gatewayBaseUrl,
                this.enabled,
                this.weight,
                at == null ? Instant.now() : at
        );
    }

    public RuntimeNode setEnabled(boolean enabled) {
        return new RuntimeNode(this.id, this.name, this.agentBaseUrl, this.gatewayBaseUrl, enabled, this.weight, this.lastHeartbeatAt);
    }

    public RuntimeNode setWeight(int weight) {
        return new RuntimeNode(this.id, this.name, this.agentBaseUrl, this.gatewayBaseUrl, this.enabled, weight, this.lastHeartbeatAt);
    }

    public RuntimeNodeId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAgentBaseUrl() {
        return agentBaseUrl;
    }

    public String getGatewayBaseUrl() {
        return gatewayBaseUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getWeight() {
        return weight;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }
}


