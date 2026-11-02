package com.kuleanasystems.auth.dto.application;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class ApplicationResponse {
    private UUID id;
    private String name;
    private UUID writeRoleId;
    private UUID readRoleId;
    private Boolean isActive;
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;
}
