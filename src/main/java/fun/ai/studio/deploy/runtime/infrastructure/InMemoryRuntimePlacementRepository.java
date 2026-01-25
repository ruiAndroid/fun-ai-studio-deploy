package fun.ai.studio.deploy.runtime.infrastructure;

import fun.ai.studio.deploy.runtime.application.RuntimePlacementRepository;
import fun.ai.studio.deploy.runtime.domain.RuntimeNodeId;
import fun.ai.studio.deploy.runtime.domain.RuntimePlacement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryRuntimePlacementRepository implements RuntimePlacementRepository {
    private final ConcurrentMap<String, RuntimePlacement> byAppId = new ConcurrentHashMap<>();

    @Override
    public RuntimePlacement save(RuntimePlacement placement) {
        if (placement == null || placement.getAppId() == null || placement.getAppId().isBlank()) {
            throw new IllegalArgumentException("appId 不能为空");
        }
        byAppId.put(placement.getAppId(), placement);
        return placement;
    }

    @Override
    public Optional<RuntimePlacement> findByAppId(String appId) {
        if (appId == null) return Optional.empty();
        return Optional.ofNullable(byAppId.get(appId));
    }

    @Override
    public List<RuntimePlacement> listByNodeId(Long nodeId, int limit, int offset) {
        int safeLimit = Math.min(Math.max(limit, 1), 2000);
        int safeOffset = Math.max(offset, 0);
        List<RuntimePlacement> all = new ArrayList<>();
        for (RuntimePlacement p : byAppId.values()) {
            if (p == null || p.getNodeId() == null) continue;
            RuntimeNodeId nid = p.getNodeId();
            if (nid.value() == null) continue;
            if (nid.value().equals(nodeId)) {
                all.add(p);
            }
        }
        all.sort(Comparator.comparing(RuntimePlacement::getAppId));
        int from = Math.min(safeOffset, all.size());
        int to = Math.min(from + safeLimit, all.size());
        return all.subList(from, to);
    }

    @Override
    public long countByNodeId(Long nodeId) {
        if (nodeId == null) return 0;
        long cnt = 0;
        for (RuntimePlacement p : byAppId.values()) {
            if (p == null || p.getNodeId() == null || p.getNodeId().value() == null) continue;
            if (p.getNodeId().value().equals(nodeId)) cnt++;
        }
        return cnt;
    }

    @Override
    public long deleteByAppId(String appId) {
        if (appId == null || appId.isBlank()) return 0;
        return byAppId.remove(appId) == null ? 0 : 1;
    }
}


