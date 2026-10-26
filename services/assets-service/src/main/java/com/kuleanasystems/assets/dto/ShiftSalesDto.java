package com.kuleanasystems.assets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftSalesDto {
    private UUID shiftSalesId;
    private UUID shiftId;
    private Instant recordedAt;
    private BigDecimal totalSales;
    private BigDecimal laborCost;
    private List<WindowSalesDto> windowSales;
}
