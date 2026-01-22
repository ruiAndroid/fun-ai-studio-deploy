package fun.ai.studio.deploy.runtime.infrastructure.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuntimePlacementJpaRepository extends JpaRepository<RuntimePlacementEntity, String> {
    Page<RuntimePlacementEntity> findByNodeIdOrderByAppIdAsc(Long nodeId, Pageable pageable);

    long countByNodeId(Long nodeId);
}


