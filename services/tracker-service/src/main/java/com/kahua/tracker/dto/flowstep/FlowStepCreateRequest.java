package com.kahua.tracker.dto.flowstep;

import lombok.Data;
import java.util.UUID;

@Data
public class FlowStepCreateRequest {
    private UUID flowId;
    private String stepName;
    private String description;
    private Integer stepSeqNum;
}