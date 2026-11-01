package com.kahua.tracker.dto.tracking;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class TrackingStatusRequest {
    private String applicationName;
    private UUID externalId;
    private UUID parentId;
    private String status;
    private String parentStatus;
    private String type;
    private UUID sessionId;
    private UUID gateId;
    private String flowName;
    private List<StepTrackingEntry> steps;
}
