package com.kahua.tracker.dto.flowsteptracking;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class FlowStepTrackingCreateRequest {
    private UUID flowTrackingId;
    private String flowStepName;
    private String status;
    private String message;
    private BigDecimal amount;
    private Integer count;
}
