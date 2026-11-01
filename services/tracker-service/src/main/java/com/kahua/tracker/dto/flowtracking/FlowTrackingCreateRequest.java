package com.kahua.tracker.dto.flowtracking;

import lombok.Data;
import java.util.UUID;

@Data
public class FlowTrackingCreateRequest {
    private String applicationName;
    private String flowName;
    private UUID externalId;
    private String status;
    private String type;
    private UUID sessionId;
    private UUID gateId;
}
