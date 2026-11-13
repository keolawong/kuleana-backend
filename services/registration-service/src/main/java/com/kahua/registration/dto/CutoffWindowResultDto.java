package com.kahua.registration.dto;

import tools.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CutoffWindowResultDto {
    private String windowId;
    private String label;
    private OffsetDateTime startsAtUtc;
    private OffsetDateTime endsAtUtc;
    private boolean allowRegister;
    private boolean allowEdit;
    private boolean allowSwapShift;
    private boolean allowCancel;
    private List<String> availableOptionIds;
    private JsonNode feeTier;
}
