package com.kahua.registration.dto;

import tools.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFormVersionCreateRequestDto {
    private JsonNode schemaJson;
    private JsonNode logicRulesJson;
    private JsonNode configJson;
    private JsonNode cutoffWindowsJson;

    @Valid
    private List<AdminOptionCreateRequestDto> options;
}
