package com.kahua.registration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.JsonNode;
import com.kahua.registration.entity.FormMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicFormResponseDto {
    private UUID id;
    private UUID orgId;
    private String slug;
    private String title;
    private String description;
    private FormMode formMode;
    private String eventTimezone;
    private OffsetDateTime goLiveAtUtc;
    private boolean live;
    private UUID activeVersionId;
    private Integer activeVersion;
    private JsonNode schemaJson;
    private JsonNode logicRulesJson;
    private JsonNode configJson;
    private CutoffWindowResultDto activeWindow;
    private List<PublicOptionDto> options;
    private List<AgreementTemplateDto> agreements;
}
