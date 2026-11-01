package com.kahua.tracker.dto.flowsteptracking;

import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class FlowStepTrackingResponse {
    private UUID id;
    private UUID flowTrackingId;
    private UUID flowStepId;
    private String status;
    private String message;
    private BigDecimal amount;
    private Integer count;
    private String createdBy;
    private OffsetDateTime createdAt;
}