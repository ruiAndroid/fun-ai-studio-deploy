package fun.ai.studio.deploy.runtime.run.config;

import fun.ai.studio.deploy.runtime.run.application.DeployAppRunRepository;
import fun.ai.studio.deploy.runtime.run.infrastructure.InMemoryDeployAppRunRepository;
import fun.ai.studio.deploy.runtime.run.infrastructure.jpa.DeployAppRunJpaRepository;
import fun.ai.studio.deploy.runtime.run.infrastructure.jpa.JpaDeployAppRunRepositoryAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Deploy App Run（last-known）存储实现切换：
 * - 默认：InMemory
 * - 开启落库：deploy.run.persistence.enabled=true + 配置 spring.datasource.url（建议 MySQL）
 */
@Configuration
public class DeployAppRunRepositoryConfig {

    @Bean
    @ConditionalOnProperty(name = "deploy.run.persistence.enabled", havingValue = "true")
    @ConditionalOnProperty(name = "spring.datasource.url")
    public DeployAppRunRepository deployAppRunRepositoryJpa(DeployAppRunJpaRepository jpa) {
        return new JpaDeployAppRunRepositoryAdapter(jpa);
    }

    @Bean
    @ConditionalOnMissingBean(DeployAppRunRepository.class)
    public DeployAppRunRepository deployAppRunRepositoryInMemoryFallback() {
        return new InMemoryDeployAppRunRepository();
    }
}


