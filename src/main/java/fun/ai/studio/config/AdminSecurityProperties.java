package fun.ai.studio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Deploy 控制面管理接口鉴权：IP 白名单 + Header Token（不依赖用户体系）。
 *
 * <pre>
 * deploy.admin.enabled=true
 * deploy.admin.allowed-ips=127.0.0.1,10.0.0.10
 * deploy.admin.token=CHANGE_ME_STRONG_ADMIN_TOKEN
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "deploy.admin")
public class AdminSecurityProperties {
    private boolean enabled = true;
    private List<String> allowedIps = new ArrayList<>();
    private String token = "";

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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}


