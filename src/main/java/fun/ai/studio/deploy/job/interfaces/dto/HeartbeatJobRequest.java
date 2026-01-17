package fun.ai.studio.deploy.job.interfaces.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class HeartbeatJobRequest {
    @NotBlank(message = "runnerId 不能为空")
    private String runnerId;

    /**
     * 续租时长（秒），从“当前时间”起延长。
     */
    @Min(value = 1, message = "extendSeconds 必须 >= 1")
    private long extendSeconds = 30;

    public String getRunnerId() {
        return runnerId;
    }

    public void setRunnerId(String runnerId) {
        this.runnerId = runnerId;
    }

    public long getExtendSeconds() {
        return extendSeconds;
    }

    public void setExtendSeconds(long extendSeconds) {
        this.extendSeconds = extendSeconds;
    }
}


