package fun.ai.studio.deploy.job.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Job 执行控制参数（控制面侧）。
 *
 * <pre>
 * deploy.job.max-running-seconds=1800
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "deploy.job")
public class JobExecutionProperties {

    /**
     * RUNNING 的最大允许时长（秒）。
     * - <=0：不启用
     * - >0：超过则自动 FAILED（在 runner heartbeat/report 时触发检查）
     */
    private long maxRunningSeconds = 0;

    public long getMaxRunningSeconds() {
        return maxRunningSeconds;
    }

    public void setMaxRunningSeconds(long maxRunningSeconds) {
        this.maxRunningSeconds = maxRunningSeconds;
    }

    public Duration maxRunningDurationOrNull() {
        if (maxRunningSeconds <= 0) return null;
        return Duration.ofSeconds(maxRunningSeconds);
    }
}


