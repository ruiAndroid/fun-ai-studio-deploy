package fun.ai.studio.deploy.runtime.run.application;

import fun.ai.studio.deploy.runtime.run.domain.DeployAppRun;

import java.util.Optional;

public interface DeployAppRunRepository {
    DeployAppRun save(DeployAppRun run);

    Optional<DeployAppRun> findByAppId(String appId);
}


