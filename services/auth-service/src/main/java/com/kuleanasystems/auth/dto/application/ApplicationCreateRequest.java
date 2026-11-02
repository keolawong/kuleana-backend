package com.kuleanasystems.auth.dto.application;

import lombok.Data;

import java.util.UUID;

@Data
public class ApplicationCreateRequest {
    private String name;
    private UUID writeRoleId;
    private UUID readRoleId;
    private Boolean isActive;
}
