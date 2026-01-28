package fun.ai.studio.deploy.runtime.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public class RuntimeNodeHeartbeatRequest {
    @NotBlank(message = "nodeName 不能为空")
    private String nodeName;

    @NotBlank(message = "agentBaseUrl 不能为空")
    private String agentBaseUrl;

    @NotBlank(message = "gatewayBaseUrl 不能为空")
    private String gatewayBaseUrl;

    /**
     * 磁盘可用百分比（0.0 ~ 100.0），用于调度选址（优先选磁盘充裕节点）。
     * 可选（兼容旧 agent）；若为 null，选址时不考虑磁盘水位。
     */
    private Double diskFreePct;

    /**
     * 磁盘可用字节数（可选，辅助展示/告警）。
     */
    private Long diskFreeBytes;

    /**
     * 当前运行的用户应用容器数（可选，用于过载保护）。
     */
    private Integer containerCount;

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
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

    public Double getDiskFreePct() {
        return diskFreePct;
    }

    public void setDiskFreePct(Double diskFreePct) {
        this.diskFreePct = diskFreePct;
    }

    public Long getDiskFreeBytes() {
        return diskFreeBytes;
    }

    public void setDiskFreeBytes(Long diskFreeBytes) {
        this.diskFreeBytes = diskFreeBytes;
    }

    public Integer getContainerCount() {
        return containerCount;
    }

    public void setContainerCount(Integer containerCount) {
        this.containerCount = containerCount;
    }
}


