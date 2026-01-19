package fun.ai.studio.deploy.runtime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Runtime 节点注册/心跳配置（Deploy 控制面侧）。
 */
@Component
@ConfigurationProperties(prefix = "deploy.runtime-node-registry")
public class RuntimeNodeRegistryProperties {
    private boolean enabled = true;
    private List<String> allowedIps = new ArrayList<>();
    private String sharedSecret = "";
    private long heartbeatStaleSeconds = 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getAllowedIps() {
        return allowedIps;
    }

    public void setAllowedIps(List<String> allowedIps) {
        this.allowedIps = allowedIps;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    public long getHeartbeatStaleSeconds() {
        return heartbeatStaleSeconds;
    }

    public void setHeartbeatStaleSeconds(long heartbeatStaleSeconds) {
        this.heartbeatStaleSeconds = heartbeatStaleSeconds;
    }

    public Duration heartbeatStaleDuration() {
        return Duration.ofSeconds(Math.max(1, heartbeatStaleSeconds));
    }
}


