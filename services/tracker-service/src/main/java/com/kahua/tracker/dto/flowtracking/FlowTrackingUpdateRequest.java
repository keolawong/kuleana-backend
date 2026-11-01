package com.kahua.tracker.dto.flowtracking;

import lombok.Data;
import java.util.Optional;
import java.util.UUID;

@Data
public class FlowTrackingUpdateRequest {
    private Optional<String> applicationName = Optional.empty();
    private Optional<String> flowName = Optional.empty();
    private Optional<UUID> externalId = Optional.empty();
    private Optional<String> status = Optional.empty();
    private Optional<String> type = Optional.empty();
    private Optional<UUID> sessionId = Optional.empty();
    private Optional<UUID> gateId = Optional.empty();
}
