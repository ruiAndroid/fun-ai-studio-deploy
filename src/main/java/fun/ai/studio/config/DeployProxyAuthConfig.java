package fun.ai.studio.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class DeployProxyAuthConfig {
    @Bean
    public FilterRegistrationBean<DeployProxyAuthFilter> deployProxyAuthFilterRegistration(DeployProxyAuthProperties props) {
        FilterRegistrationBean<DeployProxyAuthFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new DeployProxyAuthFilter(props));
        // 保护 /deploy/jobs（create/list）与 /deploy/jobs/*（get/transition/cancel 等）
        reg.addUrlPatterns("/deploy/jobs", "/deploy/jobs/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 11);
        return reg;
    }
}


