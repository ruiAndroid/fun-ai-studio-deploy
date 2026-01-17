package fun.ai.studio.deploy.job.interfaces.dto;

import fun.ai.studio.deploy.job.domain.JobStatus;
import jakarta.validation.constraints.NotNull;

public class TransitionJobRequest {
    @NotNull(message = "toStatus 不能为空")
    private JobStatus toStatus;

    /**
     * 可选：失败原因（当 toStatus=FAILED 时可传）。
     */
    private String errorMessage;

    public JobStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(JobStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}


