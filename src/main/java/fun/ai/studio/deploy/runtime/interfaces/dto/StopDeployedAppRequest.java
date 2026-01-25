package fun.ai.studio.deploy.runtime.interfaces.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 下线已部署应用请求（控制面）。
 */
public class StopDeployedAppRequest {

    @Min(value = 1, message = "userId 必须 >= 1")
    private long userId;

    @NotBlank(message = "appId 不能为空")
    private String appId;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }
}


