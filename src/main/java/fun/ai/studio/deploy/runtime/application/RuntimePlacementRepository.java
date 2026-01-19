package fun.ai.studio.deploy.runtime.application;

import fun.ai.studio.deploy.runtime.domain.RuntimePlacement;

import java.util.List;
import java.util.Optional;

public interface RuntimePlacementRepository {
    RuntimePlacement save(RuntimePlacement placement);

    Optional<RuntimePlacement> findByAppId(String appId);

    List<RuntimePlacement> listByNodeId(Long nodeId, int limit, int offset);

    long countByNodeId(Long nodeId);
}


