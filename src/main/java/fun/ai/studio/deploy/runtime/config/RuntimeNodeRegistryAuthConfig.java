package fun.ai.studio.deploy.runtime.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class RuntimeNodeRegistryAuthConfig {
    @Bean
    public FilterRegistrationBean<RuntimeNodeRegistryAuthFilter> runtimeNodeRegistryAuthFilterRegistration(
            RuntimeNodeRegistryProperties props
    ) {
        FilterRegistrationBean<RuntimeNodeRegistryAuthFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new RuntimeNodeRegistryAuthFilter(props));
        reg.addUrlPatterns("/internal/runtime-nodes/heartbeat");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 12);
        return reg;
    }
}


