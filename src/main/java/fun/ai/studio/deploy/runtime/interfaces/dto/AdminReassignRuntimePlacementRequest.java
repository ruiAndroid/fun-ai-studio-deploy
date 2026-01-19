package fun.ai.studio.deploy.runtime.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AdminReassignRuntimePlacementRequest {
    @NotBlank(message = "appId 不能为空")
    private String appId;

    @NotNull(message = "targetNodeId 不能为空")
    private Long targetNodeId;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Long getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(Long targetNodeId) {
        this.targetNodeId = targetNodeId;
    }
}


