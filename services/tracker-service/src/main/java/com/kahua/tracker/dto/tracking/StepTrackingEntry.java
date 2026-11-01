package com.kahua.tracker.dto.tracking;

import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class StepTrackingEntry {
    private String stepName;
    private String status;
    private String message;
    private BigDecimal amount;
    private Integer count;
    private OffsetDateTime updateTimestamp;
}
