package fun.ai.studio.deploy.registry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 镜像仓库清理配置（用于删除应用时清理远端镜像）。
 *
 * application.properties 示例：
 * <pre>
 * deploy.registry.cleanup.enabled=true
 * deploy.registry.cleanup.base-url=http://172.21.138.103
 * deploy.registry.cleanup.project=funaistudio
 * deploy.registry.cleanup.username=robot$cleanup
 * deploy.registry.cleanup.password=CHANGE_ME
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "deploy.registry.cleanup")
public class RegistryCleanupProperties {

    private boolean enabled = false;

    /**
     * Harbor API base URL (e.g., http://172.21.138.103)
     */
    private String baseUrl;

    /**
     * Harbor project name (e.g., funaistudio)
     */
    private String project;

    /**
     * Robot account username (e.g., robot$cleanup)
     */
    private String username;

    /**
     * Robot account password/token
     */
    private String password;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

