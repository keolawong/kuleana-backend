package com.kahua.tracker.dto.flow;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class FlowResponse {
    private UUID id;
    private String flowName;
    private UUID applicationId;
    private String description;
    private Boolean isActive;
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;
}