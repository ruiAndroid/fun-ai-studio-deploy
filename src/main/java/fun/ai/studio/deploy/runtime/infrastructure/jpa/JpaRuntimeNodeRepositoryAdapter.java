package fun.ai.studio.deploy.runtime.infrastructure.jpa;

import fun.ai.studio.deploy.runtime.application.RuntimeNodeRepository;
import fun.ai.studio.deploy.runtime.domain.RuntimeNode;
import fun.ai.studio.deploy.runtime.domain.RuntimeNodeId;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

public class JpaRuntimeNodeRepositoryAdapter implements RuntimeNodeRepository {

    private final RuntimeNodeJpaRepository jpa;

    public JpaRuntimeNodeRepositoryAdapter(RuntimeNodeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RuntimeNode save(RuntimeNode node) {
        if (node == null || node.getId() == null || node.getId().value() == null) {
            throw new IllegalArgumentException("node/id 不能为空");
        }
        RuntimeNodeEntity e = jpa.findById(node.getId().value()).orElseGet(RuntimeNodeEntity::new);
        e.setId(node.getId().value());
        e.setName(node.getName());
        e.setAgentBaseUrl(node.getAgentBaseUrl());
        e.setGatewayBaseUrl(node.getGatewayBaseUrl());
        e.setEnabled(node.isEnabled() ? 1 : 0);
        e.setWeight(node.getWeight());
        e.setLastHeartbeatAt(toLocalDateTime(node.getLastHeartbeatAt()));
        e.setDiskFreePct(node.getDiskFreePct());
        e.setDiskFreeBytes(node.getDiskFreeBytes());
        e.setContainerCount(node.getContainerCount());
        RuntimeNodeEntity saved = jpa.save(e);
        return toDomain(saved);
    }

    @Override
    public Optional<RuntimeNode> findById(RuntimeNodeId id) {
        if (id == null || id.value() == null) return Optional.empty();
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<RuntimeNode> findByName(String name) {
        if (name == null) return Optional.empty();
        return jpa.findByName(name).map(this::toDomain);
    }

    @Override
    public List<RuntimeNode> listAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    private RuntimeNode toDomain(RuntimeNodeEntity e) {
        if (e == null) return null;
        RuntimeNode base = RuntimeNode.create(
                new RuntimeNodeId(e.getId()),
                e.getName(),
                e.getAgentBaseUrl(),
                e.getGatewayBaseUrl(),
                e.getEnabled() != null && e.getEnabled() == 1,
                e.getWeight() == null ? 100 : e.getWeight()
        );
        LocalDateTime hb = e.getLastHeartbeatAt();
        if (hb == null) return base;
        Instant at = hb.atZone(ZoneId.systemDefault()).toInstant();
        return base.heartbeat(at, e.getAgentBaseUrl(), e.getGatewayBaseUrl(), e.getDiskFreePct(), e.getDiskFreeBytes(), e.getContainerCount());
    }

    private static LocalDateTime toLocalDateTime(Instant at) {
        if (at == null) return null;
        return LocalDateTime.ofInstant(at, ZoneId.systemDefault());
    }
}


