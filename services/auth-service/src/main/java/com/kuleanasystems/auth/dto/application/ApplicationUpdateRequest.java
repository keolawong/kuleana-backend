package com.kuleanasystems.auth.dto.application;

import lombok.Data;

import java.util.Optional;
import java.util.UUID;

@Data
public class ApplicationUpdateRequest {
    private Optional<String> name = Optional.empty();
    private Optional<UUID> writeRoleId = Optional.empty();
    private Optional<UUID> readRoleId = Optional.empty();
    private Optional<Boolean> isActive = Optional.empty();
}
