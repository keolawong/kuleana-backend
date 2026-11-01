package com.kahua.tracker.dto.flowtracking;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class FlowTrackingResponse {
    private UUID id;
    private UUID flowId;
    private UUID externalId;
    private String status;
    private String type;
    private UUID sessionId;
    private UUID gateId;
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;
}