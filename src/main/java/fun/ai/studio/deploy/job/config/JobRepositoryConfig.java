package fun.ai.studio.deploy.job.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fun.ai.studio.deploy.job.application.JobRepository;
import fun.ai.studio.deploy.job.infrastructure.InMemoryJobRepository;
import fun.ai.studio.deploy.job.infrastructure.jpa.JobJpaRepository;
import fun.ai.studio.deploy.job.infrastructure.jpa.JpaJobRepositoryAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Job 队列存储实现切换：
 * - 默认：InMemory（重启丢数据）
 * - 开启落库：deploy.job.persistence.enabled=true + 配置 spring.datasource.url（建议 MySQL）
 */
@Configuration
public class JobRepositoryConfig {

    @Bean
    @ConditionalOnProperty(name = "deploy.job.persistence.enabled", havingValue = "true")
    @ConditionalOnProperty(name = "spring.datasource.url")
    public JobRepository jobRepositoryJpa(JobJpaRepository jpa, ObjectMapper om) {
        return new JpaJobRepositoryAdapter(jpa, om);
    }

    @Bean
    @ConditionalOnMissingBean(JobRepository.class)
    public JobRepository jobRepositoryInMemoryFallback() {
        return new InMemoryJobRepository();
    }
}


