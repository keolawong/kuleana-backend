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
public class OperationTotalsDto {
    private Integer sessionNum;
    private String day;
    private String time;
    private int tickets;
    private BigDecimal monetaryAmt;
    private BigDecimal banks;
    private BigDecimal expectedValue;
    private BigDecimal netSales;
    private BigDecimal overUnder;
}
