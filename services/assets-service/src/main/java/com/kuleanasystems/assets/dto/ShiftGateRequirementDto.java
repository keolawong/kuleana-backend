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
public class ShiftGateRequirementDto {
    private UUID requirementId;
    private UUID shiftId;
    private UUID gateId;
    private String gateName;
    private String jobRole;
    private Integer workersNeeded;
    private Integer minWorkers;
    private Integer happyWorkers;
    private Integer priority;

    public Integer getPlannedWorkers() {
        return workersNeeded;
    }

    public void setPlannedWorkers(Integer plannedWorkers) {
        this.workersNeeded = plannedWorkers;
    }
}
