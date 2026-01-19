package fun.ai.studio.deploy.runtime.domain;

/**
 * appId -> runtimeNode 的粘性落点（后续可持久化到 DB）。
 */
public class RuntimePlacement {
    private final String appId;
    private final RuntimeNodeId nodeId;
    private final long lastActiveAt;

    public RuntimePlacement(String appId, RuntimeNodeId nodeId, long lastActiveAt) {
        this.appId = appId;
        this.nodeId = nodeId;
        this.lastActiveAt = lastActiveAt;
    }

    public String getAppId() {
        return appId;
    }

    public RuntimeNodeId getNodeId() {
        return nodeId;
    }

    public long getLastActiveAt() {
        return lastActiveAt;
    }
}


