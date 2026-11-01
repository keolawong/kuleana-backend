package com.kahua.tracker.dto.flowstep;

import lombok.Data;
import java.util.Optional;
import java.util.UUID;

@Data
public class FlowStepUpdateRequest {
    private Optional<UUID> flowId = Optional.empty();
    private Optional<String> stepName = Optional.empty();
    private Optional<String> description = Optional.empty();
    private Optional<Integer> stepSeqNum = Optional.empty();
}