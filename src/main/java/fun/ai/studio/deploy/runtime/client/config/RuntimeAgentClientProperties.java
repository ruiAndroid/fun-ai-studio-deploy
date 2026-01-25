package fun.ai.studio.deploy.runtime.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 控制面 -> runtime-agent 调用配置。
 *
 * <pre>
 * deploy.runtime-agent.token=CHANGE_ME
 * deploy.runtime-agent.timeout-seconds=10
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "deploy.runtime-agent")
public class RuntimeAgentClientProperties {

    /**
     * runtime-agent 鉴权 Token（Header：X-Runtime-Token）
     */
    private String token = "";

    /**
     * HTTP 调用超时（秒）
     */
    private int timeoutSeconds = 10;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}


