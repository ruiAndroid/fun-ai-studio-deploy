package fun.ai.studio.deploy.runtime.run.domain;

/**
 * Deploy 控制面：应用运行态 last-known（类似 API 的 fun_ai_workspace_run）。
 *
 * 说明：
 * - 这里的“真相”仍以 runtime-agent/容器探测为准
 * - DB 主要用于：展示/审计/服务重启后的最后状态
 */
public class DeployAppRun {

    private final String appId;
    private final Long nodeId;
    private final String lastJobId;
    private final String lastJobStatus;
    private final String lastError;
    private final Long lastDeployedAt;
    private final Long lastActiveAt;

    public DeployAppRun(String appId,
                        Long nodeId,
                        String lastJobId,
                        String lastJobStatus,
                        String lastError,
                        Long lastDeployedAt,
                        Long lastActiveAt) {
        this.appId = appId;
        this.nodeId = nodeId;
        this.lastJobId = lastJobId;
        this.lastJobStatus = lastJobStatus;
        this.lastError = lastError;
        this.lastDeployedAt = lastDeployedAt;
        this.lastActiveAt = lastActiveAt;
    }

    public String getAppId() {
        return appId;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public String getLastJobId() {
        return lastJobId;
    }

    public String getLastJobStatus() {
        return lastJobStatus;
    }

    public String getLastError() {
        return lastError;
    }

    public Long getLastDeployedAt() {
        return lastDeployedAt;
    }

    public Long getLastActiveAt() {
        return lastActiveAt;
    }
}


