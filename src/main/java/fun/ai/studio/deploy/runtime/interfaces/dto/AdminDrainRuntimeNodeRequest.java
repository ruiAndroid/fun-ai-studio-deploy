package fun.ai.studio.deploy.runtime.interfaces.dto;

import jakarta.validation.constraints.NotNull;

public class AdminDrainRuntimeNodeRequest {
    @NotNull(message = "sourceNodeId 不能为空")
    private Long sourceNodeId;
    @NotNull(message = "targetNodeId 不能为空")
    private Long targetNodeId;
    private Integer limit;

    public Long getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(Long sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public Long getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(Long targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}


