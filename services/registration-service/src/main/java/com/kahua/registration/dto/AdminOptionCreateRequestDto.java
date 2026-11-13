package com.kahua.registration.dto;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOptionCreateRequestDto {
    private String externalRefId;

    @NotBlank
    private String optionType;

    @NotBlank
    private String label;

    private JsonNode metadataJson;

    private Integer maxCapacity;
}
