package com.kuleanasystems.auth.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserApplicationUpdateRequest {
    private List<ApplicationAssignment> applications;

    @Data
    public static class ApplicationAssignment {
        private String appName;
        private String status;
        private String primaryRole;
    }
}
