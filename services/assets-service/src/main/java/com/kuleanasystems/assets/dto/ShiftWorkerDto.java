package com.kuleanasystems.assets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftWorkerDto {
    private UUID shiftWorkerId;
    private UUID shiftId;
    private String roleName;
    private BigDecimal hourlyRate;
    private Integer count;
}
