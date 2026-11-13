package com.kahua.registration.dto;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAgreementCreateRequestDto {
    private UUID orgId;

    @NotBlank
    private String templateKey;

    @NotBlank
    private String type;

    private String ageRule;

    private String requirementMode;

    @NotBlank
    private String title;

    private JsonNode summaryBulletsJson;

    @NotBlank
    private String bodyHtml;

    private Boolean isActive;
}
