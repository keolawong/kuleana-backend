package com.kahua.tracker.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UserRoleUpdateRequest {

    private List<RoleAssignment> roles;

    @Data
    public static class RoleAssignment {
        private UUID roleId;
        private String status;
    }
}
