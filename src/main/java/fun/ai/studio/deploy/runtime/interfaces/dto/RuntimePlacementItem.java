package fun.ai.studio.deploy.runtime.interfaces.dto;

import lombok.Data;

@Data
public class RuntimePlacementItem {
    private String appId;
    private Long nodeId;
    private Long lastActiveAt;
}


