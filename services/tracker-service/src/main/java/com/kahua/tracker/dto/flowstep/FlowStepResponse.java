package com.kahua.tracker.dto.flowstep;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class FlowStepResponse {
    private UUID id;
    private UUID flowId;
    private String stepName;
    private String description;
    private Integer stepSeqNum;
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;
}