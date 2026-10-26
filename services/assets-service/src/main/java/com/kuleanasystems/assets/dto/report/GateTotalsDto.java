package com.kuleanasystems.assets.dto.report;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GateTotalsDto {
    private BigDecimal bankAmount;
    private int ticketsSold;
    private BigDecimal ticketValue;
    private BigDecimal ticketBankValue;
    private BigDecimal cashCcAmount;
    private BigDecimal difference;
}
