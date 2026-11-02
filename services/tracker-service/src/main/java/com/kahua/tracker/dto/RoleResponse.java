package com.kahua.tracker.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class RoleResponse {
    private UUID id;
    private String name;
    private String description;
    private String appName;
    private Boolean isActive;
    private OffsetDateTime createdAt;
}
