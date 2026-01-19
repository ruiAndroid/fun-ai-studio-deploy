package fun.ai.studio.deploy.runtime.infrastructure;

import fun.ai.studio.deploy.runtime.application.RuntimeNodeRepository;
import fun.ai.studio.deploy.runtime.domain.RuntimeNode;
import fun.ai.studio.deploy.runtime.domain.RuntimeNodeId;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryRuntimeNodeRepository implements RuntimeNodeRepository {
    private final ConcurrentMap<Long, RuntimeNode> byId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> idByName = new ConcurrentHashMap<>();

    @Override
    public RuntimeNode save(RuntimeNode node) {
        if (node == null || node.getId() == null || node.getId().value() == null) {
            throw new IllegalArgumentException("node/id 不能为空");
        }
        byId.put(node.getId().value(), node);
        if (node.getName() != null) {
            idByName.put(node.getName(), node.getId().value());
        }
        return node;
    }

    @Override
    public Optional<RuntimeNode> findById(RuntimeNodeId id) {
        if (id == null || id.value() == null) return Optional.empty();
        return Optional.ofNullable(byId.get(id.value()));
    }

    @Override
    public Optional<RuntimeNode> findByName(String name) {
        if (name == null) return Optional.empty();
        Long id = idByName.get(name);
        if (id == null) return Optional.empty();
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<RuntimeNode> listAll() {
        return new ArrayList<>(byId.values());
    }
}


