package fun.ai.studio.deploy.runtime.interfaces;

import fun.ai.studio.common.Result;
import fun.ai.studio.deploy.runtime.application.RuntimePlacementService;
import fun.ai.studio.deploy.runtime.config.RuntimeNodeRegistryProperties;
import fun.ai.studio.deploy.runtime.domain.RuntimeNode;
import fun.ai.studio.deploy.runtime.domain.RuntimePlacement;
import fun.ai.studio.deploy.runtime.interfaces.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/runtime-nodes")
public class AdminRuntimeNodeController {

    private final RuntimePlacementService svc;
    private final RuntimeNodeRegistryProperties registryProps;

    public AdminRuntimeNodeController(RuntimePlacementService svc, RuntimeNodeRegistryProperties registryProps) {
        this.svc = svc;
        this.registryProps = registryProps;
    }

    @GetMapping("/list")
    public Result<List<RuntimeNodeResponse>> list() {
        List<RuntimeNode> nodes = svc.listNodes();
        List<RuntimeNodeResponse> out = nodes.stream()
                .map(n -> RuntimeNodeResponse.from(n, healthOf(n)))
                .collect(Collectors.toList());
        return Result.success(out);
    }

    @PostMapping("/upsert")
    public Result<RuntimeNodeResponse> upsert(@Valid @RequestBody AdminUpsertRuntimeNodeRequest req) {
        RuntimeNode n = svc.upsertNode(req.getName(), req.getAgentBaseUrl(), req.getGatewayBaseUrl(), req.getEnabled(), req.getWeight());
        return Result.success(RuntimeNodeResponse.from(n, healthOf(n)));
    }

    @PostMapping("/set-enabled")
    public Result<String> setEnabled(@RequestParam String name, @RequestParam boolean enabled) {
        RuntimeNode n = svc.upsertNode(name, null, null, enabled, null);
        return Result.success(n.isEnabled() ? "enabled" : "disabled");
    }

    @GetMapping("/placements")
    public Result<RuntimePlacementsResponse> placements(@RequestParam Long nodeId,
                                                        @RequestParam(defaultValue = "0") int offset,
                                                        @RequestParam(defaultValue = "200") int limit) {
        List<RuntimePlacement> ps = svc.listPlacements(nodeId, limit, offset);
        List<RuntimePlacementItem> items = ps.stream().map(p -> {
            RuntimePlacementItem it = new RuntimePlacementItem();
            it.setAppId(p.getAppId());
            it.setNodeId(p.getNodeId() == null ? null : p.getNodeId().value());
            it.setLastActiveAt(p.getLastActiveAt());
            return it;
        }).collect(Collectors.toList());
        RuntimePlacementsResponse resp = new RuntimePlacementsResponse();
        resp.setNodeId(nodeId);
        resp.setTotal(svc.countPlacements(nodeId));
        resp.setItems(items);
        return Result.success(resp);
    }

    @PostMapping("/reassign")
    public Result<String> reassign(@Valid @RequestBody AdminReassignRuntimePlacementRequest req) {
        svc.reassign(req.getAppId(), req.getTargetNodeId());
        return Result.success("ok");
    }

    @PostMapping("/drain")
    public Result<Map<String, Object>> drain(@Valid @RequestBody AdminDrainRuntimeNodeRequest req) {
        int moved = svc.drain(req.getSourceNodeId(), req.getTargetNodeId(), req.getLimit() == null ? 100 : req.getLimit());
        return Result.success(Map.of(
                "moved", moved,
                "sourceNodeId", req.getSourceNodeId(),
                "targetNodeId", req.getTargetNodeId()
        ));
    }

    private String healthOf(RuntimeNode n) {
        if (n == null) return "UNKNOWN";
        if (registryProps == null || !registryProps.isEnabled()) return "UNKNOWN";
        Instant at = n.getLastHeartbeatAt();
        if (at == null) return "STALE";
        return at.isAfter(Instant.now().minus(registryProps.heartbeatStaleDuration())) ? "HEALTHY" : "STALE";
    }
}


