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

    /**
     * 可选：Runner 上报当前执行阶段（用于 UI 展示与排障）。
     * 例如：CLONE / BUILD / PUSH / DEPLOY / DONE
     */
    private String phase;

    /**
     * 可选：阶段描述（尽量简短，避免把完整日志塞进来）。
     */
    private String phaseMessage;

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

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getPhaseMessage() {
        return phaseMessage;
    }

    public void setPhaseMessage(String phaseMessage) {
        this.phaseMessage = phaseMessage;
    }
}


