package fun.ai.studio.deploy.runtime.application;

import fun.ai.studio.deploy.runtime.domain.RuntimePlacement;

import java.util.List;
import java.util.Optional;

public interface RuntimePlacementRepository {
    RuntimePlacement save(RuntimePlacement placement);

    Optional<RuntimePlacement> findByAppId(String appId);

    List<RuntimePlacement> listByNodeId(Long nodeId, int limit, int offset);

    long countByNodeId(Long nodeId);

    /**
     * 清理 appId -> nodeId 粘性落点记录（删除应用后的控制面数据清理）。
     *
     * @return 删除条数（0/1）
     */
    long deleteByAppId(String appId);
}


