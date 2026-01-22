package fun.ai.studio.deploy.runtime.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RuntimeNodeJpaRepository extends JpaRepository<RuntimeNodeEntity, Long> {
    Optional<RuntimeNodeEntity> findByName(String name);
}


