package fun.ai.studio.deploy.runtime.infrastructure.jpa;

import fun.ai.studio.deploy.runtime.application.RuntimePlacementRepository;
import fun.ai.studio.deploy.runtime.domain.RuntimeNodeId;
import fun.ai.studio.deploy.runtime.domain.RuntimePlacement;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;

public class JpaRuntimePlacementRepositoryAdapter implements RuntimePlacementRepository {

    private final RuntimePlacementJpaRepository jpa;
    private final EntityManager em;

    public JpaRuntimePlacementRepositoryAdapter(RuntimePlacementJpaRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    public RuntimePlacement save(RuntimePlacement placement) {
        if (placement == null || placement.getAppId() == null || placement.getAppId().isBlank()) {
            throw new IllegalArgumentException("appId 不能为空");
        }
        if (placement.getNodeId() == null || placement.getNodeId().value() == null) {
            throw new IllegalArgumentException("nodeId 不能为空");
        }
        RuntimePlacementEntity e = jpa.findById(placement.getAppId()).orElseGet(RuntimePlacementEntity::new);
        e.setAppId(placement.getAppId()); // appId 为主键（upsert）
        e.setNodeId(placement.getNodeId().value());
        e.setLastActiveAt(placement.getLastActiveAt());
        RuntimePlacementEntity saved = jpa.save(e);
        return toDomain(saved);
    }

    @Override
    public Optional<RuntimePlacement> findByAppId(String appId) {
        if (appId == null) return Optional.empty();
        return jpa.findById(appId).map(this::toDomain);
    }

    @Override
    public List<RuntimePlacement> listByNodeId(Long nodeId, int limit, int offset) {
        int safeLimit = Math.min(Math.max(limit, 1), 2000);
        int safeOffset = Math.max(offset, 0);
        TypedQuery<RuntimePlacementEntity> q = em.createQuery(
                "select p from RuntimePlacementEntity p where p.nodeId = :nodeId order by p.appId asc",
                RuntimePlacementEntity.class
        );
        q.setParameter("nodeId", nodeId);
        q.setFirstResult(safeOffset);
        q.setMaxResults(safeLimit);
        List<RuntimePlacementEntity> items = q.getResultList();
        return items.stream().map(this::toDomain).toList();
    }

    @Override
    public long countByNodeId(Long nodeId) {
        if (nodeId == null) return 0;
        return jpa.countByNodeId(nodeId);
    }

    private RuntimePlacement toDomain(RuntimePlacementEntity e) {
        if (e == null) return null;
        return new RuntimePlacement(e.getAppId(), new RuntimeNodeId(e.getNodeId()), e.getLastActiveAt());
    }
}


