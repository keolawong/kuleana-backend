package com.kuleanasystems.assets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgGatePreferenceDto {
    private UUID preferenceId;
    private UUID shiftId;
    private UUID gateId;
    private String gateName;
    private String organization;
    private Integer priority;
    private String notes;
}
