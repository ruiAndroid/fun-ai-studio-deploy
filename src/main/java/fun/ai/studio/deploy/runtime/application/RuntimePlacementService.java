package fun.ai.studio.deploy.runtime.application;

import fun.ai.studio.common.ConflictException;
import fun.ai.studio.common.NotFoundException;
import fun.ai.studio.deploy.runtime.config.RuntimeNodeRegistryProperties;
import fun.ai.studio.deploy.runtime.domain.RuntimeNode;
import fun.ai.studio.deploy.runtime.domain.RuntimeNodeId;
import fun.ai.studio.deploy.runtime.domain.RuntimePlacement;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * runtime 节点选址（控制面）：appId -> runtimeNode（粘性落点）。
 *
 * 说明：
 * - 默认实现为 InMemory repository（不依赖 DB，便于先跑通闭环）
 * - 生产建议开启 DB 落库（避免 Deploy 重启丢 runtime 节点与 placements）
 *   - deploy.runtime.persistence.enabled=true
 *   - 配置 spring.datasource.*
 */
@Service
public class RuntimePlacementService {
    private final RuntimeNodeRepository nodeRepo;
    private final RuntimePlacementRepository placementRepo;
    private final RuntimeNodeRegistryProperties registryProps;

    public RuntimePlacementService(RuntimeNodeRepository nodeRepo,
                                   RuntimePlacementRepository placementRepo,
                                   RuntimeNodeRegistryProperties registryProps) {
        this.nodeRepo = nodeRepo;
        this.placementRepo = placementRepo;
        this.registryProps = registryProps;
    }

    public RuntimePlacement ensurePlacement(String appId) {
        if (!StringUtils.hasText(appId)) throw new IllegalArgumentException("appId 不能为空");
        return placementRepo.findByAppId(appId).orElseGet(() -> {
            RuntimeNode chosen = chooseNodeForApp(appId);
            RuntimePlacement p = new RuntimePlacement(appId, chosen.getId(), System.currentTimeMillis());
            return placementRepo.save(p);
        });
    }

    public RuntimeNode resolveNode(String appId) {
        RuntimePlacement p = ensurePlacement(appId);
        RuntimeNodeId nodeId = p.getNodeId();
        if (nodeId == null || nodeId.value() == null) throw new ConflictException("placement nodeId is null: appId=" + appId);

        RuntimeNode node = nodeRepo.findById(nodeId)
                .orElseThrow(() -> new NotFoundException("runtime node not found: nodeId=" + nodeId.value()));
        if (!node.isEnabled()) throw new ConflictException("runtime node disabled: nodeId=" + nodeId.value());
        if (!StringUtils.hasText(node.getAgentBaseUrl()) || !StringUtils.hasText(node.getGatewayBaseUrl())) {
            throw new ConflictException("runtime node baseUrl empty: nodeId=" + nodeId.value());
        }
        if (!isHeartbeatFresh(node)) {
            throw new ConflictException("runtime node unhealthy (heartbeat stale), please manual-drain: nodeId=" + nodeId.value());
        }
        return node;
    }

    public List<RuntimeNode> listNodes() {
        return nodeRepo.listAll();
    }

    public RuntimeNode upsertNode(String name, String agentBaseUrl, String gatewayBaseUrl, Boolean enabled, Integer weight) {
        if (!StringUtils.hasText(name)) throw new IllegalArgumentException("name 不能为空");
        RuntimeNode node = nodeRepo.findByName(name.trim()).orElse(null);
        if (node == null) {
            long nextId = System.nanoTime(); // in-memory：用纳秒时间做唯一 id；后续 DB 会替换
            RuntimeNode created = RuntimeNode.create(new RuntimeNodeId(nextId), name.trim(), agentBaseUrl, gatewayBaseUrl,
                    enabled == null ? true : enabled,
                    weight == null ? 100 : weight);
            return nodeRepo.save(created);
        }
        RuntimeNode updated = node;
        if (enabled != null) updated = updated.setEnabled(enabled);
        if (weight != null) updated = updated.setWeight(weight);
        if (agentBaseUrl != null || gatewayBaseUrl != null) {
            updated = updated.heartbeat(node.getLastHeartbeatAt(), agentBaseUrl, gatewayBaseUrl);
        }
        return nodeRepo.save(updated);
    }

    public RuntimeNode heartbeat(String name, String agentBaseUrl, String gatewayBaseUrl) {
        if (!StringUtils.hasText(name)) throw new IllegalArgumentException("nodeName 不能为空");
        RuntimeNode node = nodeRepo.findByName(name.trim()).orElse(null);
        if (node == null) {
            RuntimeNode created = RuntimeNode.create(new RuntimeNodeId(System.nanoTime()), name.trim(), agentBaseUrl, gatewayBaseUrl, true, 100);
            RuntimeNode hb = created.heartbeat(Instant.now(), agentBaseUrl, gatewayBaseUrl);
            return nodeRepo.save(hb);
        }
        RuntimeNode hb = node.heartbeat(Instant.now(), agentBaseUrl, gatewayBaseUrl);
        return nodeRepo.save(hb);
    }

    public List<RuntimePlacement> listPlacements(Long nodeId, int limit, int offset) {
        if (nodeId == null) throw new IllegalArgumentException("nodeId 不能为空");
        int safeLimit = Math.min(Math.max(limit, 1), 2000);
        int safeOffset = Math.max(offset, 0);
        return placementRepo.listByNodeId(nodeId, safeLimit, safeOffset);
    }

    public long countPlacements(Long nodeId) {
        if (nodeId == null) return 0;
        return placementRepo.countByNodeId(nodeId);
    }

    public void reassign(String appId, Long targetNodeId) {
        if (!StringUtils.hasText(appId)) throw new IllegalArgumentException("appId 不能为空");
        if (targetNodeId == null) throw new IllegalArgumentException("targetNodeId 不能为空");
        RuntimeNode target = nodeRepo.findById(new RuntimeNodeId(targetNodeId))
                .orElseThrow(() -> new NotFoundException("target node not found: nodeId=" + targetNodeId));
        if (!target.isEnabled()) throw new ConflictException("target node disabled");
        RuntimePlacement p = new RuntimePlacement(appId, new RuntimeNodeId(targetNodeId), System.currentTimeMillis());
        placementRepo.save(p);
    }

    public int drain(Long sourceNodeId, Long targetNodeId, int limit) {
        if (sourceNodeId == null || targetNodeId == null) throw new IllegalArgumentException("source/target 不能为空");
        if (Objects.equals(sourceNodeId, targetNodeId)) throw new IllegalArgumentException("source/target 不能相同");
        RuntimeNode target = nodeRepo.findById(new RuntimeNodeId(targetNodeId))
                .orElseThrow(() -> new NotFoundException("target node not found: nodeId=" + targetNodeId));
        if (!target.isEnabled()) throw new ConflictException("target node disabled");

        List<RuntimePlacement> ps = placementRepo.listByNodeId(sourceNodeId, Math.min(Math.max(limit, 1), 2000), 0);
        int moved = 0;
        for (RuntimePlacement p : ps) {
            if (p == null || !StringUtils.hasText(p.getAppId())) continue;
            placementRepo.save(new RuntimePlacement(p.getAppId(), new RuntimeNodeId(targetNodeId), System.currentTimeMillis()));
            moved++;
        }
        return moved;
    }

    private RuntimeNode chooseNodeForApp(String appId) {
        List<RuntimeNode> nodes = nodeRepo.listAll().stream()
                .filter(Objects::nonNull)
                .filter(RuntimeNode::isEnabled)
                .filter(n -> n.getId() != null && n.getId().value() != null)
                .filter(n -> StringUtils.hasText(n.getAgentBaseUrl()))
                .filter(n -> StringUtils.hasText(n.getGatewayBaseUrl()))
                .filter(this::isHeartbeatFresh)
                .sorted(Comparator.comparing(n -> n.getId().value()))
                .toList();
        if (nodes.isEmpty()) throw new ConflictException("no valid runtime nodes");
        int idx = Math.floorMod(appId.hashCode(), nodes.size());
        return nodes.get(idx);
    }

    private boolean isHeartbeatFresh(RuntimeNode node) {
        if (node == null) return false;
        if (registryProps == null || !registryProps.isEnabled()) return true;
        if (node.getLastHeartbeatAt() == null) return false;
        Instant now = Instant.now();
        return node.getLastHeartbeatAt().isAfter(now.minus(registryProps.heartbeatStaleDuration()));
    }
}


