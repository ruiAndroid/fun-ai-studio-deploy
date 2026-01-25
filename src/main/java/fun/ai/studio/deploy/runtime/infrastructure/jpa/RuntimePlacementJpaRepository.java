package fun.ai.studio.deploy.runtime.infrastructure.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface RuntimePlacementJpaRepository extends JpaRepository<RuntimePlacementEntity, String> {
    Page<RuntimePlacementEntity> findByNodeIdOrderByAppIdAsc(Long nodeId, Pageable pageable);

    long countByNodeId(Long nodeId);

    @Transactional
    long deleteByAppId(String appId);
}


