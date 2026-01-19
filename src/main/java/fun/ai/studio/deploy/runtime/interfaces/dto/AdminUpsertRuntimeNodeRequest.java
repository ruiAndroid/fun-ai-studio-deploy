package fun.ai.studio.deploy.runtime.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminUpsertRuntimeNodeRequest {
    @NotBlank(message = "name 不能为空")
    private String name;

    @NotBlank(message = "agentBaseUrl 不能为空")
    private String agentBaseUrl;

    @NotBlank(message = "gatewayBaseUrl 不能为空")
    private String gatewayBaseUrl;

    private Boolean enabled;
    private Integer weight;

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
}


