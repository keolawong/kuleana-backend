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
public class GatePresetQuotaDto {
    private UUID presetQuotaId;
    private String presetType;
    private UUID gateId;
    private String gateName;
    private String jobRole;
    private Integer plannedWorkers;
    private Integer minWorkers;
    private Integer happyWorkers;
    private Integer priority;
}
