package fun.ai.studio.deploy.runtime.run.infrastructure.jpa;

import fun.ai.studio.deploy.runtime.run.application.DeployAppRunRepository;
import fun.ai.studio.deploy.runtime.run.domain.DeployAppRun;

import java.util.Optional;

public class JpaDeployAppRunRepositoryAdapter implements DeployAppRunRepository {

    private final DeployAppRunJpaRepository jpa;

    public JpaDeployAppRunRepositoryAdapter(DeployAppRunJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public DeployAppRun save(DeployAppRun run) {
        if (run == null || run.getAppId() == null || run.getAppId().isBlank()) {
            throw new IllegalArgumentException("appId 不能为空");
        }
        DeployAppRunEntity e = jpa.findById(run.getAppId()).orElseGet(DeployAppRunEntity::new);
        e.setAppId(run.getAppId());
        e.setNodeId(run.getNodeId());
        e.setLastJobId(run.getLastJobId());
        e.setLastJobStatus(run.getLastJobStatus());
        e.setLastError(run.getLastError());
        e.setLastDeployedAt(run.getLastDeployedAt());
        e.setLastActiveAt(run.getLastActiveAt());
        DeployAppRunEntity saved = jpa.save(e);
        return toDomain(saved);
    }

    @Override
    public Optional<DeployAppRun> findByAppId(String appId) {
        if (appId == null) return Optional.empty();
        return jpa.findById(appId).map(this::toDomain);
    }

    private DeployAppRun toDomain(DeployAppRunEntity e) {
        if (e == null) return null;
        return new DeployAppRun(
                e.getAppId(),
                e.getNodeId(),
                e.getLastJobId(),
                e.getLastJobStatus(),
                e.getLastError(),
                e.getLastDeployedAt(),
                e.getLastActiveAt()
        );
    }
}


