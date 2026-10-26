package com.kuleanasystems.assets.dto.report;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GateSalesSummaryDto {
    private UUID gateId;
    private String gateName;
    private Integer displayOrder;
    private int cashQty;
    private BigDecimal cashValue;
    private int ccQty;
    private BigDecimal ccValue;
    private int totalQty;
    private BigDecimal totalValue;
}
