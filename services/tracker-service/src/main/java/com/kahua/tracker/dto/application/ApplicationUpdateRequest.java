package com.kahua.tracker.dto.application;

import lombok.Data;
import java.util.UUID;
import java.util.Optional;

@Data
public class ApplicationUpdateRequest {
    private Optional<String> name = Optional.empty();
    private Optional<UUID> writeRoleId = Optional.empty();
    private Optional<UUID> readRoleId = Optional.empty();
    private Optional<Boolean> isActive = Optional.empty();
}