package fun.ai.studio.deploy.runtime.config;

import fun.ai.studio.deploy.runtime.application.RuntimeNodeRepository;
import fun.ai.studio.deploy.runtime.application.RuntimePlacementRepository;
import fun.ai.studio.deploy.runtime.infrastructure.InMemoryRuntimeNodeRepository;
import fun.ai.studio.deploy.runtime.infrastructure.InMemoryRuntimePlacementRepository;
import fun.ai.studio.deploy.runtime.infrastructure.jpa.*;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * runtime registry/placement 存储实现切换：
 * - 默认：InMemory（不依赖 DB，部署控制面重启会丢数据）
 * - 开启落库：deploy.runtime.persistence.enabled=true + 配置 spring.datasource.url（建议 MySQL）
 */
@Configuration
public class RuntimeRepositoryConfig {

    @Bean
    @ConditionalOnProperty(name = "deploy.runtime.persistence.enabled", havingValue = "true")
    @ConditionalOnProperty(name = "spring.datasource.url")
    public RuntimeNodeRepository runtimeNodeRepositoryJpa(RuntimeNodeJpaRepository jpa) {
        return new JpaRuntimeNodeRepositoryAdapter(jpa);
    }

    @Bean
    @ConditionalOnProperty(name = "deploy.runtime.persistence.enabled", havingValue = "true")
    @ConditionalOnProperty(name = "spring.datasource.url")
    public RuntimePlacementRepository runtimePlacementRepositoryJpa(RuntimePlacementJpaRepository jpa, EntityManager em) {
        return new JpaRuntimePlacementRepositoryAdapter(jpa, em);
    }

    @Bean
    @ConditionalOnMissingBean(RuntimeNodeRepository.class)
    public RuntimeNodeRepository runtimeNodeRepositoryInMemoryFallback() {
        return new InMemoryRuntimeNodeRepository();
    }

    @Bean
    @ConditionalOnMissingBean(RuntimePlacementRepository.class)
    public RuntimePlacementRepository runtimePlacementRepositoryInMemoryFallback() {
        return new InMemoryRuntimePlacementRepository();
    }
}


