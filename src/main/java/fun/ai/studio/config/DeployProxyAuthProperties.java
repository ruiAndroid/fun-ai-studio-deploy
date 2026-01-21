package fun.ai.studio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * API -> Deploy 内部调用鉴权（不依赖用户体系）：
 * - 可选 IP 白名单
 * - 共享密钥 Header（API 侧 DeployClient 会发送：X-DEPLOY-SECRET）
 *
 * <pre>
 * deploy.proxy-auth.enabled=true
 * deploy.proxy-auth.allowed-ips=127.0.0.1,10.0.0.10
 * deploy.proxy-auth.shared-secret=CHANGE_ME_STRONG_DEPLOY_PROXY_SECRET
 * </pre>
 *
 * 说明：
 * - 本鉴权只用于保护“控制面调用接口”（例如创建/查询/列表 Job）
 * - Runner 的 claim/heartbeat/report 默认放行，避免影响执行面
 */
@Component
@ConfigurationProperties(prefix = "deploy.proxy-auth")
public class DeployProxyAuthProperties {
    private boolean enabled = false;
    private List<String> allowedIps = new ArrayList<>();
    private String sharedSecret = "";

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
}


