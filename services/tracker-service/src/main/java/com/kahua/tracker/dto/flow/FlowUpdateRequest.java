package com.kahua.tracker.dto.flow;

import lombok.Data;
import java.util.Optional;
import java.util.UUID;

@Data
public class FlowUpdateRequest {
    private Optional<String> flowName = Optional.empty();
    private Optional<UUID> applicationId = Optional.empty();
    private Optional<String> description = Optional.empty();
    private Optional<Boolean> isActive = Optional.empty();
}