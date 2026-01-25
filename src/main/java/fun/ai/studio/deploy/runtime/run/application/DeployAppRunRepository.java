package fun.ai.studio.deploy.runtime.run.application;

import fun.ai.studio.deploy.runtime.run.domain.DeployAppRun;

import java.util.Optional;

public interface DeployAppRunRepository {
    DeployAppRun save(DeployAppRun run);

    Optional<DeployAppRun> findByAppId(String appId);

    /**
     * 清理 last-known 运行态记录。
     *
     * @return 删除条数（0/1）
     */
    long deleteByAppId(String appId);
}


