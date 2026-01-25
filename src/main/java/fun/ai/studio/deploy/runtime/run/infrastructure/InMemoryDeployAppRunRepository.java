package fun.ai.studio.deploy.runtime.run.infrastructure;

import fun.ai.studio.deploy.runtime.run.application.DeployAppRunRepository;
import fun.ai.studio.deploy.runtime.run.domain.DeployAppRun;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryDeployAppRunRepository implements DeployAppRunRepository {

    private final ConcurrentMap<String, DeployAppRun> byAppId = new ConcurrentHashMap<>();

    @Override
    public DeployAppRun save(DeployAppRun run) {
        if (run == null || run.getAppId() == null || run.getAppId().isBlank()) {
            throw new IllegalArgumentException("appId 不能为空");
        }
        byAppId.put(run.getAppId(), run);
        return run;
    }

    @Override
    public Optional<DeployAppRun> findByAppId(String appId) {
        if (appId == null) return Optional.empty();
        return Optional.ofNullable(byAppId.get(appId));
    }

    @Override
    public long deleteByAppId(String appId) {
        if (appId == null || appId.isBlank()) return 0;
        return byAppId.remove(appId) == null ? 0 : 1;
    }
}


