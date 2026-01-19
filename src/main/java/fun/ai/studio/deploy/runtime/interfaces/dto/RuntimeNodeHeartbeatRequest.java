package fun.ai.studio.deploy.runtime.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public class RuntimeNodeHeartbeatRequest {
    @NotBlank(message = "nodeName 不能为空")
    private String nodeName;

    @NotBlank(message = "agentBaseUrl 不能为空")
    private String agentBaseUrl;

    @NotBlank(message = "gatewayBaseUrl 不能为空")
    private String gatewayBaseUrl;

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
}


