package com.kuleanasystems.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserOrganizationResponse {
    private UUID id;
    private UUID userId;
    private String username;
    private String firstName;
    private String lastName;
    private UUID orgId;
    private String orgName;
    private String orgSlug;
    private String role;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
