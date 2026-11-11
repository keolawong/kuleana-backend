package com.kuleanasystems.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationConfigResponse {
    private String appName;
    private String displayTitle;
    private boolean requireCaptcha;
    private String turnstileSiteKey;
    private String organizationSlug;
    private String organizationName;
    private List<String> defaultRoles;
    private boolean allowRegistration;
}
