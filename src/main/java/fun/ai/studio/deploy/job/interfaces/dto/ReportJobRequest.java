package fun.ai.studio.deploy.job.interfaces.dto;

import fun.ai.studio.deploy.job.domain.JobStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ReportJobRequest {
    @NotBlank(message = "runnerId 不能为空")
    private String runnerId;

    @NotNull(message = "status 不能为空")
    private JobStatus status;

    private String errorMessage;

    public String getRunnerId() {
        return runnerId;
    }

    public void setRunnerId(String runnerId) {
        this.runnerId = runnerId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}


