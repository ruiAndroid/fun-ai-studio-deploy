package fun.ai.studio.deploy.job.interfaces;

import fun.ai.studio.common.Result;
import fun.ai.studio.deploy.job.application.JobService;
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
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/deploy/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public Result<JobResponse> create(@Valid @RequestBody CreateJobRequest req) {
        Job job = jobService.create(req.getType(), req.getPayload());
        return Result.success(JobResponse.from(job));
    }

    @GetMapping("/{jobId}")
    public Result<JobResponse> get(@PathVariable String jobId) {
        return Result.success(JobResponse.from(jobService.get(jobId)));
    }

    @GetMapping
    public Result<List<JobResponse>> list(@RequestParam(defaultValue = "50") int limit) {
        return Result.success(
                jobService.list(limit).stream()
                        .map(JobResponse::from)
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
        Optional<Job> job = jobService.claimNext(req.getRunnerId(), Duration.ofSeconds(req.getLeaseSeconds()));
        return Result.success(job.map(JobResponse::from).orElse(null));
    }

    /**
     * Runner 心跳续租：延长 leaseExpireAt。
     */
    @PostMapping("/{jobId}/heartbeat")
    public Result<JobResponse> heartbeat(@PathVariable String jobId, @Valid @RequestBody HeartbeatJobRequest req) {
        Job job = jobService.heartbeat(jobId, req.getRunnerId(), Duration.ofSeconds(req.getExtendSeconds()));
        return Result.success(JobResponse.from(job));
    }

    /**
     * Runner 上报执行结果：SUCCEEDED/FAILED/CANCELLED。
     */
    @PostMapping("/{jobId}/report")
    public Result<JobResponse> report(@PathVariable String jobId, @Valid @RequestBody ReportJobRequest req) {
        Job job = jobService.report(jobId, req.getRunnerId(), req.getStatus(), req.getErrorMessage());
        return Result.success(JobResponse.from(job));
    }
}


