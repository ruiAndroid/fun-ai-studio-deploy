package fun.ai.studio.deploy.job.interfaces.dto;

import fun.ai.studio.deploy.job.domain.JobType;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class CreateJobRequest {
    @NotNull(message = "type 不能为空")
    private JobType type;

    /**
     * 任意扩展字段（例如：repoUrl/commit/appId/env 等）。
     */
    private Map<String, Object> payload;

    public JobType getType() {
        return type;
    }

    public void setType(JobType type) {
        this.type = type;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}


