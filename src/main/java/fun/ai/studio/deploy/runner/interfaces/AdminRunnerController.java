package fun.ai.studio.deploy.runner.interfaces;

import fun.ai.studio.common.Result;
import fun.ai.studio.deploy.runner.application.RunnerRegistryService;
import fun.ai.studio.deploy.runner.interfaces.dto.RunnerSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/runners")
@Tag(name = "Admin Runners", description = "Runner 在线状态（运维）。鉴权：X-Admin-Token +（可选）deploy.admin.allowed-ips")
public class AdminRunnerController {

    private final RunnerRegistryService registry;

    public AdminRunnerController(RunnerRegistryService registry) {
        this.registry = registry;
    }

    @GetMapping("/list")
    @Operation(summary = "Runner 列表（最近活跃时间 + 在线判定）")
    public Result<List<RunnerSummary>> list() {
        return Result.success(registry == null ? List.of() : registry.list());
    }
}


