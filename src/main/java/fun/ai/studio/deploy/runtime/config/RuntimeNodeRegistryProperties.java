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

    /**
     * 调度策略：disk-aware（磁盘水位优先） / hash（一致性哈希，旧策略）
     */
    private String placementStrategy = "disk-aware";

    /**
     * 硬阈值：磁盘可用百分比低于此值的节点不可选（仅 disk-aware 策略生效）
     */
    private double diskFreeMinPct = 15.0;

    /**
     * 软阈值：磁盘可用百分比低于此值的节点标记为 DRAINING（不接新部署，但已有 placement 不迁移）
     */
    private double diskFreeDrainPct = 25.0;

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

    public String getPlacementStrategy() {
        return placementStrategy;
    }

    public void setPlacementStrategy(String placementStrategy) {
        this.placementStrategy = placementStrategy;
    }

    public double getDiskFreeMinPct() {
        return diskFreeMinPct;
    }

    public void setDiskFreeMinPct(double diskFreeMinPct) {
        this.diskFreeMinPct = diskFreeMinPct;
    }

    public double getDiskFreeDrainPct() {
        return diskFreeDrainPct;
    }

    public void setDiskFreeDrainPct(double diskFreeDrainPct) {
        this.diskFreeDrainPct = diskFreeDrainPct;
    }
}


