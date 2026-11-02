package com.kahua.tracker.dto;

import lombok.Data;
import java.util.Optional;

@Data
public class RoleUpdateRequest {
    private Optional<String> name = Optional.empty();
    private Optional<String> description = Optional.empty();
    private Optional<String> appName = Optional.empty();
    private Optional<Boolean> isActive = Optional.empty();
}
