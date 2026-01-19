package fun.ai.studio.deploy.runtime.interfaces.dto;

import lombok.Data;

import java.util.List;

@Data
public class RuntimePlacementsResponse {
    private Long nodeId;
    private Long total;
    private List<RuntimePlacementItem> items;
}


