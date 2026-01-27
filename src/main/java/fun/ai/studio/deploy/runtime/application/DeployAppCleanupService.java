package fun.ai.studio.deploy.runtime.application;

import fun.ai.studio.deploy.job.application.JobRepository;
import fun.ai.studio.deploy.job.domain.Job;
import fun.ai.studio.deploy.registry.HarborRegistryClient;
import fun.ai.studio.deploy.runtime.client.RuntimeAgentClient;
import fun.ai.studio.deploy.runtime.domain.RuntimeNode;
import fun.ai.studio.deploy.runtime.run.application.DeployAppRunService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
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
    private final HarborRegistryClient harborClient; // optional (enabled via property)

    public DeployAppCleanupService(JobRepository jobRepo,
                                   RuntimePlacementService placementService,
                                   RuntimeAgentClient runtimeAgentClient,
                                   DeployAppPurgeService purgeService,
                                   DeployAppRunService appRunService,
                                   ObjectProvider<HarborRegistryClient> harborClientProvider) {
        this.jobRepo = jobRepo;
        this.placementService = placementService;
        this.runtimeAgentClient = runtimeAgentClient;
        this.purgeService = purgeService;
        this.appRunService = appRunService;
        this.harborClient = harborClientProvider == null ? null : harborClientProvider.getIfAvailable();
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

        // 1.1 可选：删除 Harbor 远端镜像（best-effort）
        Map<String, Object> registryResult = new HashMap<>();
        try {
            if (harborClient != null && harborClient.isEnabled() && imageTags != null && !imageTags.isEmpty()) {
                List<String> deleted = new ArrayList<>();
                List<String> failed = new ArrayList<>();
                Set<String> repos = new HashSet<>();
                for (String img : imageTags) {
                    ImageRef ref = parseImage(img);
                    if (ref == null || ref.repository == null || ref.reference == null) continue;
                    repos.add(ref.repository);
                    boolean ok = harborClient.deleteArtifact(ref.repository, ref.reference);
                    if (ok) deleted.add(img);
                    else failed.add(img);
                }

                // best-effort: delete repository entries too (so UI won't keep empty repos)
                List<String> repoDeleted = new ArrayList<>();
                List<String> repoFailed = new ArrayList<>();
                for (String r : repos) {
                    boolean ok = harborClient.deleteRepository(r);
                    if (ok) repoDeleted.add(r);
                    else repoFailed.add(r);
                }
                registryResult.put("enabled", true);
                registryResult.put("deleted", deleted);
                registryResult.put("failed", failed);
                registryResult.put("repoDeleted", repoDeleted);
                registryResult.put("repoFailed", repoFailed);
            } else {
                registryResult.put("enabled", false);
            }
        } catch (Exception e) {
            registryResult.put("enabled", true);
            registryResult.put("error", e.getMessage());
        }
        out.put("registry", registryResult);

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

    private static class ImageRef {
        final String repository; // e.g. funaistudio/u10000021-app20000470
        final String reference;  // tag or digest, e.g. latest or sha256:...

        private ImageRef(String repository, String reference) {
            this.repository = repository;
            this.reference = reference;
        }
    }

    /**
     * Parse image like:
     *  - 172.21.138.103/funaistudio/u10000021-app20000470:latest
     *  - 172.21.138.103/funaistudio/u10000021-app20000470@sha256:...
     * into repository + reference for Harbor API.
     */
    private static ImageRef parseImage(String image) {
        if (image == null || image.isBlank()) return null;
        String s = image.trim();

        // Split reference first (digest has precedence)
        String repoPart = s;
        String reference = null;
        int at = s.indexOf('@');
        if (at > 0) {
            repoPart = s.substring(0, at);
            reference = s.substring(at + 1);
        } else {
            int lastColon = s.lastIndexOf(':');
            // If lastColon is after last '/', treat as tag separator
            int lastSlash = s.lastIndexOf('/');
            if (lastColon > 0 && lastColon > lastSlash) {
                repoPart = s.substring(0, lastColon);
                reference = s.substring(lastColon + 1);
            } else {
                reference = "latest";
            }
        }

        // Strip registry host (first segment before '/')
        int firstSlash = repoPart.indexOf('/');
        if (firstSlash <= 0 || firstSlash >= repoPart.length() - 1) return null;
        String repository = repoPart.substring(firstSlash + 1); // keep project/repo
        if (repository.isBlank() || reference == null || reference.isBlank()) return null;
        return new ImageRef(repository, reference);
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

