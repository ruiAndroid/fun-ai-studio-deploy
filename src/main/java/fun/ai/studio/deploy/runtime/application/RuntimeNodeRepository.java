package fun.ai.studio.deploy.runtime.application;

import fun.ai.studio.deploy.runtime.domain.RuntimeNode;
import fun.ai.studio.deploy.runtime.domain.RuntimeNodeId;

import java.util.List;
import java.util.Optional;

public interface RuntimeNodeRepository {
    RuntimeNode save(RuntimeNode node);

    Optional<RuntimeNode> findById(RuntimeNodeId id);

    Optional<RuntimeNode> findByName(String name);

    List<RuntimeNode> listAll();
}


