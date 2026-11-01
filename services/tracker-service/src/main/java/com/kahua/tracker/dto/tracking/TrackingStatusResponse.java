package com.kahua.tracker.dto.tracking;

import com.kahua.tracker.dto.flowsteptracking.FlowStepTrackingResponse;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class TrackingStatusResponse {
    private UUID id;
    private UUID parentId;
    private UUID flowId;
    private String flowName;
    private UUID externalId;
    private String status;
    private String type;
    private UUID sessionId;
    private UUID gateId;
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;
    private List<FlowStepTrackingResponse> steps;
    private List<TrackingStatusResponse> actions;
}
