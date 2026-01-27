package fun.ai.studio.deploy.runtime.application;

import fun.ai.studio.deploy.job.application.JobRepository;
import fun.ai.studio.deploy.job.domain.Job;
import fun.ai.studio.deploy.runtime.client.RuntimeAgentClient;
import fun.ai.studio.deploy.runtime.domain.RuntimeNode;
import fun.ai.studio.deploy.runtime.run.application.DeployAppRunService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 删除应用后的完整清理：
 * 1. 从 job 历史收集 image tags
 * 2. 调用 runtime-agent delete（删除容器 + 清理本地镜像）
 * 3. 调用 purge 清理控制面数据（job/appRun/placement）
 */
@Service
public class DeployAppCleanupService {

    private static final Logger log = LoggerFactory.getLogger(DeployAppCleanupService.class);

    private final JobRepository jobRepo;
    private final RuntimePlacementService placementService;
    private final RuntimeAgentClient runtimeAgentClient;
    private final DeployAppPurgeService purgeService;
    private final DeployAppRunService appRunService;

    public DeployAppCleanupService(JobRepository jobRepo,
                                   RuntimePlacementService placementService,
                                   RuntimeAgentClient runtimeAgentClient,
                                   DeployAppPurgeService purgeService,
                                   DeployAppRunService appRunService) {
        this.jobRepo = jobRepo;
        this.placementService = placementService;
        this.runtimeAgentClient = runtimeAgentClient;
        this.purgeService = purgeService;
        this.appRunService = appRunService;
    }

    /**
     * 完整清理：runtime 容器/镜像 + 控制面数据。
     *
     * @param userId 用户 ID
     * @param appId  应用 ID
     * @return 清理结果
     */
    public Map<String, Object> cleanup(String userId, String appId) {
        Map<String, Object> out = new HashMap<>();
        out.put("userId", userId);
        out.put("appId", appId);

        // 1. 从 job 历史收集 image tags（供后续 Harbor 清理或日志记录）
        Set<String> imageTags = collectImageTags(appId);
        out.put("imageTags", imageTags);

        // 2. 调用 runtime-agent delete（best-effort）
        Map<String, Object> runtimeResult = new HashMap<>();
        try {
            RuntimeNode node = placementService.resolveNode(appId);
            if (node != null && node.getAgentBaseUrl() != null) {
                Map resp = runtimeAgentClient.deleteApp(node.getAgentBaseUrl(), userId, appId);
                runtimeResult.put("agentBaseUrl", node.getAgentBaseUrl());
                runtimeResult.put("response", resp);
                runtimeResult.put("success", true);
            } else {
                runtimeResult.put("skipped", true);
                runtimeResult.put("reason", "no placement found");
            }
        } catch (Exception e) {
            log.warn("runtime-agent delete failed: userId={}, appId={}, err={}", userId, appId, e.getMessage());
            runtimeResult.put("success", false);
            runtimeResult.put("error", e.getMessage());
        }
        out.put("runtime", runtimeResult);

        // 3. 标记 app 为 STOPPED（best-effort）
        try {
            if (appRunService != null) appRunService.touchStopped(appId);
        } catch (Exception ignore) {
        }

        // 4. purge 控制面数据
        Map<String, Object> purgeResult = new HashMap<>();
        try {
            purgeResult = purgeService.purge(appId);
        } catch (Exception e) {
            log.warn("purge failed: appId={}, err={}", appId, e.getMessage());
            purgeResult.put("error", e.getMessage());
        }
        out.put("purge", purgeResult);

        return out;
    }

    /**
     * 从 job 历史收集该 appId 所有构建过的 image tags。
     */
    private Set<String> collectImageTags(String appId) {
        if (jobRepo == null) return Collections.emptySet();
        try {
            List<Job> jobs = jobRepo.listByAppId(appId, 100);
            if (jobs == null || jobs.isEmpty()) return Collections.emptySet();
            return jobs.stream()
                    .map(Job::getPayload)
                    .filter(Objects::nonNull)
                    .map(p -> {
                        // 优先 payload.image；否则从 acrRegistry/acrNamespace/userId/appId/imageTag 拼接
                        Object img = p.get("image");
                        if (img != null && !String.valueOf(img).isBlank()) {
                            return String.valueOf(img);
                        }
                        Object reg = p.get("acrRegistry");
                        Object ns = p.get("acrNamespace");
                        Object uid = p.get("userId");
                        Object tag = p.get("imageTag");
                        if (reg == null || ns == null || uid == null) return null;
                        String t = tag == null ? "latest" : String.valueOf(tag);
                        return String.format("%s/%s/u%s-app%s:%s", reg, ns, uid, appId, t);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("collectImageTags failed: appId={}, err={}", appId, e.getMessage());
            return Collections.emptySet();
        }
    }
}

