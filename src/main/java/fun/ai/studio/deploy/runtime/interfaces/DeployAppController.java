package fun.ai.studio.deploy.runtime.interfaces;

import fun.ai.studio.common.Result;
import fun.ai.studio.deploy.runtime.application.RuntimePlacementService;
import fun.ai.studio.deploy.runtime.client.RuntimeAgentClient;
import fun.ai.studio.deploy.runtime.domain.RuntimeNode;
import fun.ai.studio.deploy.runtime.run.application.DeployAppRunService;
import fun.ai.studio.deploy.runtime.interfaces.dto.StopDeployedAppRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 控制面：应用运维接口（下线/状态等）。
 * <p>
 * 说明：这些接口建议仅允许 API 内部调用（通过 DeployProxyAuthFilter 校验 X-DEPLOY-SECRET）。
 */
@RestController
@RequestMapping("/deploy/apps")
public class DeployAppController {

    private final RuntimePlacementService placementService;
    private final RuntimeAgentClient runtimeAgentClient;
    private final DeployAppRunService appRunService;

    public DeployAppController(RuntimePlacementService placementService,
                               RuntimeAgentClient runtimeAgentClient,
                               DeployAppRunService appRunService) {
        this.placementService = placementService;
        this.runtimeAgentClient = runtimeAgentClient;
        this.appRunService = appRunService;
    }

    /**
     * 下线（stop）已部署应用：删除容器。
     */
    @PostMapping("/stop")
    public Result<Map<String, Object>> stop(@Valid @RequestBody StopDeployedAppRequest req) {
        String appId = req.getAppId();
        RuntimeNode node = placementService.resolveNode(appId);
        Map<String, Object> out = new HashMap<>();
        out.put("appId", appId);
        out.put("userId", req.getUserId());
        out.put("nodeId", node.getId() == null ? null : node.getId().value());
        out.put("agentBaseUrl", node.getAgentBaseUrl());

        Map resp = runtimeAgentClient.stopApp(node.getAgentBaseUrl(), appId);
        out.put("runtime", resp);

        try {
            if (appRunService != null) appRunService.touchStopped(appId);
        } catch (Exception ignore) {
        }
        return Result.success(out);
    }
}


