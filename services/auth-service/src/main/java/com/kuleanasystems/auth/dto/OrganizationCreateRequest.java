package com.kuleanasystems.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationCreateRequest {
    private String slug;
    private String name;
    private String description;
    private String logoUrl;
    private String status;
}
