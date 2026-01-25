package fun.ai.studio.deploy.runtime.application;

import fun.ai.studio.deploy.job.application.JobRepository;
import fun.ai.studio.deploy.runtime.run.application.DeployAppRunRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 删除应用后的控制面数据清理（不操作 runtime 容器）。
 */
@Service
public class DeployAppPurgeService {

    private final JobRepository jobRepo;
    private final DeployAppRunRepository appRunRepo;
    private final RuntimePlacementRepository placementRepo;

    public DeployAppPurgeService(JobRepository jobRepo,
                                 DeployAppRunRepository appRunRepo,
                                 RuntimePlacementRepository placementRepo) {
        this.jobRepo = jobRepo;
        this.appRunRepo = appRunRepo;
        this.placementRepo = placementRepo;
    }

    public Map<String, Object> purge(String appId) {
        Map<String, Object> out = new HashMap<>();
        out.put("appId", appId);

        long jobs = 0;
        long runs = 0;
        long placements = 0;
        try {
            if (jobRepo != null) jobs = jobRepo.deleteByAppId(appId);
        } catch (Exception ignore) {
        }
        try {
            if (appRunRepo != null) runs = appRunRepo.deleteByAppId(appId);
        } catch (Exception ignore) {
        }
        try {
            if (placementRepo != null) placements = placementRepo.deleteByAppId(appId);
        } catch (Exception ignore) {
        }

        out.put("deletedJobs", jobs);
        out.put("deletedAppRuns", runs);
        out.put("deletedPlacements", placements);
        return out;
    }
}


