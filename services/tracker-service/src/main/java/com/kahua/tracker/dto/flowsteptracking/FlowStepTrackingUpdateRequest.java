package com.kahua.tracker.dto.flowsteptracking;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Data
public class FlowStepTrackingUpdateRequest {
    private Optional<UUID> flowTrackingId = Optional.empty();
    private Optional<String> flowStepName = Optional.empty();
    private Optional<String> status = Optional.empty();
    private Optional<String> message = Optional.empty();
    private Optional<BigDecimal> amount = Optional.empty();
    private Optional<Integer> count = Optional.empty();
}
