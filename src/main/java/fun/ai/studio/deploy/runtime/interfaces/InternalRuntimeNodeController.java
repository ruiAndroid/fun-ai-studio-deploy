package fun.ai.studio.deploy.runtime.interfaces;

import fun.ai.studio.common.Result;
import fun.ai.studio.deploy.runtime.application.RuntimePlacementService;
import fun.ai.studio.deploy.runtime.domain.RuntimeNode;
import fun.ai.studio.deploy.runtime.interfaces.dto.RuntimeNodeHeartbeatRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/runtime-nodes")
public class InternalRuntimeNodeController {
    private final RuntimePlacementService svc;

    public InternalRuntimeNodeController(RuntimePlacementService svc) {
        this.svc = svc;
    }

    @PostMapping("/heartbeat")
    public Result<RuntimeNode> heartbeat(@Valid @RequestBody RuntimeNodeHeartbeatRequest req) {
        RuntimeNode node = svc.heartbeat(
                req.getNodeName(),
                req.getAgentBaseUrl(),
                req.getGatewayBaseUrl(),
                req.getDiskFreePct(),
                req.getDiskFreeBytes(),
                req.getContainerCount()
        );
        return Result.success(node);
    }
}


