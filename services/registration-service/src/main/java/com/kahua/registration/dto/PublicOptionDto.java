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
public class PublicOptionDto {
    private UUID id;
    private String externalRefId;
    private String optionType;
    private String label;
    private JsonNode metadataJson;
    private Integer maxCapacity;
    private Integer reservedCount;
    private Integer spotsRemaining;
}
