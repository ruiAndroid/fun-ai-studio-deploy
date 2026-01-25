package fun.ai.studio.deploy.job.interfaces;

import fun.ai.studio.common.Result;
import fun.ai.studio.deploy.job.application.JobService;
import fun.ai.studio.deploy.runner.application.RunnerRegistryService;
import fun.ai.studio.deploy.runtime.application.RuntimePlacementService;
import fun.ai.studio.deploy.runtime.domain.RuntimeNode;
import fun.ai.studio.deploy.job.domain.Job;
import fun.ai.studio.deploy.job.interfaces.dto.CreateJobRequest;
import fun.ai.studio.deploy.job.interfaces.dto.ClaimJobRequest;
import fun.ai.studio.deploy.job.interfaces.dto.HeartbeatJobRequest;
import fun.ai.studio.deploy.job.interfaces.dto.JobResponse;
import fun.ai.studio.deploy.job.interfaces.dto.ReportJobRequest;
import fun.ai.studio.deploy.job.interfaces.dto.TransitionJobRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import fun.ai.studio.deploy.runtime.run.application.DeployAppRunService;

@RestController
@RequestMapping("/deploy/jobs")
public class JobController {

    private final JobService jobService;
    private final RuntimePlacementService runtimePlacementService;
    private final RunnerRegistryService runnerRegistry;
    private final DeployAppRunService appRunService;

    public JobController(JobService jobService,
                         RuntimePlacementService runtimePlacementService,
                         RunnerRegistryService runnerRegistry,
                         DeployAppRunService appRunService) {
        this.jobService = jobService;
        this.runtimePlacementService = runtimePlacementService;
        this.runnerRegistry = runnerRegistry;
        this.appRunService = appRunService;
    }

    @PostMapping
    public Result<JobResponse> create(@Valid @RequestBody CreateJobRequest req) {
        Job job = jobService.create(req.getType(), req.getPayload());
        return Result.success(JobResponse.from(job));
    }

    @GetMapping("/{jobId}")
    public Result<JobResponse> get(@PathVariable String jobId) {
        Job job = jobService.get(jobId);
        RuntimeNode node = resolveRuntimeNode(job);
        String deployUrl = buildPreviewUrl(job, node);
        return Result.success(JobResponse.from(job, node, deployUrl));
    }

    @GetMapping
    public Result<List<JobResponse>> list(@RequestParam(defaultValue = "50") int limit) {
        return Result.success(
                jobService.list(limit).stream()
                        .map(j -> {
                            RuntimeNode node = resolveRuntimeNode(j);
                            String deployUrl = buildPreviewUrl(j, node);
                            return JobResponse.from(j, node, deployUrl);
                        })
                        .collect(Collectors.toList())
        );
    }

    @PostMapping("/{jobId}/transition")
    public Result<JobResponse> transition(@PathVariable String jobId,
                                          @Valid @RequestBody TransitionJobRequest req) {
        Job job = jobService.transition(jobId, req.getToStatus(), req.getErrorMessage());
        return Result.success(JobResponse.from(job));
    }

    @PostMapping("/{jobId}/cancel")
    public Result<JobResponse> cancel(@PathVariable String jobId) {
        return Result.success(JobResponse.from(jobService.cancel(jobId)));
    }

    /**
     * Runner 轮询领取任务：返回一个可执行 job；若无任务则 data=null（仍返回 200）。
     */
    @PostMapping("/claim")
    public Result<JobResponse> claim(@Valid @RequestBody ClaimJobRequest req) {
        if (runnerRegistry != null) runnerRegistry.touch(req == null ? null : req.getRunnerId());
        Optional<Job> job = jobService.claimNext(req.getRunnerId(), Duration.ofSeconds(req.getLeaseSeconds()));
        if (job.isEmpty()) return Result.success(null);

        Job j = job.get();
        // A 方案：claim 时返回 runtime 节点信息（由控制面决定）
        RuntimeNode node = null;
        try {
            Object appId = j.getPayload() == null ? null : j.getPayload().get("appId");
            if (appId != null) {
                node = runtimePlacementService.resolveNode(String.valueOf(appId));
            }
        } catch (Exception ignore) {
        }
        String deployUrl = buildPreviewUrl(j, node);
        return Result.success(JobResponse.from(j, node, deployUrl));
    }

    /**
     * Runner 心跳续租：延长 leaseExpireAt。
     */
    @PostMapping("/{jobId}/heartbeat")
    public Result<JobResponse> heartbeat(@PathVariable String jobId, @Valid @RequestBody HeartbeatJobRequest req) {
        if (runnerRegistry != null) runnerRegistry.touch(req == null ? null : req.getRunnerId());
        Job job = jobService.heartbeat(
                jobId,
                req.getRunnerId(),
                Duration.ofSeconds(req.getExtendSeconds()),
                req.getPhase(),
                req.getPhaseMessage()
        );
        return Result.success(JobResponse.from(job));
    }

    /**
     * Runner 上报执行结果：SUCCEEDED/FAILED/CANCELLED。
     */
    @PostMapping("/{jobId}/report")
    public Result<JobResponse> report(@PathVariable String jobId, @Valid @RequestBody ReportJobRequest req) {
        if (runnerRegistry != null) runnerRegistry.touch(req == null ? null : req.getRunnerId());
        Job job = jobService.report(jobId, req.getRunnerId(), req.getStatus(), req.getErrorMessage());
        try {
            if (appRunService != null) appRunService.touchFromJob(job);
        } catch (Exception ignore) {
        }
        RuntimeNode node = resolveRuntimeNode(job);
        String deployUrl = buildPreviewUrl(job, node);
        return Result.success(JobResponse.from(job, node, deployUrl));
    }

    private RuntimeNode resolveRuntimeNode(Job job) {
        if (job == null) return null;
        try {
            Map<String, Object> payload = job.getPayload();
            Object appId = payload == null ? null : payload.get("appId");
            if (appId == null) return null;
            return runtimePlacementService.resolveNode(String.valueOf(appId));
        } catch (Exception ignore) {
            return null;
        }
    }

    private String buildPreviewUrl(Job job, RuntimeNode node) {
        if (job == null || node == null) return null;
        try {
            String base = node.getGatewayBaseUrl();
            if (base == null || base.isBlank()) return null;
            base = base.trim();
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);

            Map<String, Object> payload = job.getPayload();
            String appId = String.valueOf(payload == null ? null : payload.get("appId"));
            if (appId == null || appId.isBlank()) return null;

            Object bp = payload == null ? null : payload.get("basePath");
            String path = (bp == null ? "" : String.valueOf(bp)).trim();
            if (path.isBlank()) path = "/apps/" + appId;
            if (!path.startsWith("/")) path = "/" + path;
            if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
            return base + path + "/";
        } catch (Exception ignore) {
            return null;
        }
    }
}


