package com.kuleanasystems.auth.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class RoleCreateRequest {
    private String name;
    private String description;
    private String appName;
    private List<UUID> pageIds;
}
