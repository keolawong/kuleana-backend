package com.kahua.registration.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import tools.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionSubmitRequestDto {
    private UUID formVersionId;
    private String participantKey;
    private JsonNode participantProfile;
    private JsonNode answers;
    private List<UUID> selectedOptionIds;
    private String expectedWindowId;
    private Boolean waitlistOnFull;

    @Valid
    private List<SignedAgreementRequestDto> signedAgreements;

    @Builder.Default
    private Map<String, Object> unknownFields = new HashMap<>();

    @JsonAnySetter
    public void captureUnknownField(String key, Object value) {
        this.unknownFields.put(key, value);
    }
}
