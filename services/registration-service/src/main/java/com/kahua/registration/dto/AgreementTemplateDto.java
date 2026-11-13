package com.kahua.registration.dto;

import tools.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementTemplateDto {
    private UUID id;
    private String templateKey;
    private Integer version;
    private String type;
    private String ageRule;
    private String requirementMode;
    private String title;
    private JsonNode summaryBulletsJson;
    private String bodyHtml;
    private String contentHash;
}
