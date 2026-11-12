package com.kuleanasystems.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkAccountRequest {
    private String username;
    private String password;
    private String organizationSlug;
    private String appName;
    private String captchaToken;
}
