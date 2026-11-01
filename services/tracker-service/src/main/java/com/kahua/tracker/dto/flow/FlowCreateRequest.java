package com.kahua.tracker.dto.flow;

import lombok.Data;
import java.util.UUID;

@Data
public class FlowCreateRequest {
    private String flowName;
    private UUID applicationId;
    private String description;
    private Boolean isActive;
}