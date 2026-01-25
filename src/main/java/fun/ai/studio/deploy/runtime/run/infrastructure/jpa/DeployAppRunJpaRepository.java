package fun.ai.studio.deploy.runtime.run.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface DeployAppRunJpaRepository extends JpaRepository<DeployAppRunEntity, String> {
    @Transactional
    long deleteByAppId(String appId);
}


