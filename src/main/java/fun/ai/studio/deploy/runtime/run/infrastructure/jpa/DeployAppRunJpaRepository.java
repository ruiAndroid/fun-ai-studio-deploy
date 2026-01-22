package fun.ai.studio.deploy.runtime.run.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeployAppRunJpaRepository extends JpaRepository<DeployAppRunEntity, String> {
}


