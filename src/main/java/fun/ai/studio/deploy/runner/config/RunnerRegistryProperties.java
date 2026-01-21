package fun.ai.studio.deploy.runner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Runner 在线状态判定配置。
 *
 * <pre>
 * deploy.runner-registry.heartbeat-stale-seconds=60
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "deploy.runner-registry")
public class RunnerRegistryProperties {
    private long heartbeatStaleSeconds = 60;

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


