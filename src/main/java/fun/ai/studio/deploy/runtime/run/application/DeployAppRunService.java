package fun.ai.studio.deploy.runtime.run.application;

import fun.ai.studio.deploy.job.domain.Job;
import fun.ai.studio.deploy.runtime.application.RuntimePlacementService;
import fun.ai.studio.deploy.runtime.domain.RuntimePlacement;
import fun.ai.studio.deploy.runtime.run.domain.DeployAppRun;
import org.springframework.stereotype.Service;

/**
 * Deploy App Run（last-known）写入逻辑：在 Runner report 时更新。
 */
@Service
public class DeployAppRunService {

    private final DeployAppRunRepository repo;
    private final RuntimePlacementService placementService;

    public DeployAppRunService(DeployAppRunRepository repo, RuntimePlacementService placementService) {
        this.repo = repo;
        this.placementService = placementService;
    }

    public void touchFromJob(Job job) {
        if (job == null || job.getPayload() == null) return;
        Object appIdObj = job.getPayload().get("appId");
        if (appIdObj == null) return;
        String appId = String.valueOf(appIdObj);
        if (appId.isBlank()) return;

        RuntimePlacement p = placementService.ensurePlacement(appId);
        Long nodeId = (p.getNodeId() == null ? null : p.getNodeId().value());

        long now = System.currentTimeMillis();
        Long lastDeployedAt = job.getStatus() != null && job.getStatus().name().equals("SUCCEEDED") ? now : null;
        DeployAppRun run = new DeployAppRun(
                appId,
                nodeId,
                job.getId() == null ? null : job.getId().value(),
                job.getStatus() == null ? null : job.getStatus().name(),
                job.getErrorMessage(),
                lastDeployedAt,
                now
        );
        repo.save(run);
    }
}


