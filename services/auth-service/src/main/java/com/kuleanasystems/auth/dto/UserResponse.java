package com.kuleanasystems.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private Boolean locked;
    private OffsetDateTime createdAt;
    private List<RoleInfo> roles;
    private List<ApplicationInfo> applications;
    private List<String> apps;

    @Data
    @Builder
    public static class RoleInfo {
        private UUID roleId;
        private String name;
        private String status;
    }

    @Data
    @Builder
    public static class ApplicationInfo {
        private String appName;
        private String status;
        private String primaryRole;
    }
}
