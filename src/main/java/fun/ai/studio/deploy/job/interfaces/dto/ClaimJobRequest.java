package fun.ai.studio.deploy.job.interfaces.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class ClaimJobRequest {
    @NotBlank(message = "runnerId 不能为空")
    private String runnerId;

    /**
     * 租约时长（秒）。
     */
    @Min(value = 1, message = "leaseSeconds 必须 >= 1")
    private long leaseSeconds = 30;

    public String getRunnerId() {
        return runnerId;
    }

    public void setRunnerId(String runnerId) {
        this.runnerId = runnerId;
    }

    public long getLeaseSeconds() {
        return leaseSeconds;
    }

    public void setLeaseSeconds(long leaseSeconds) {
        this.leaseSeconds = leaseSeconds;
    }
}


