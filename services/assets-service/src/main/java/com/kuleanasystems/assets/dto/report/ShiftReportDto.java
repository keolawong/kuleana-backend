package com.kuleanasystems.assets.dto.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftReportDto {
    private UUID shiftId;
    private String shiftName;
    private Instant startTime;
    private Instant endTime;
    private UUID operationId;
    private String operationName;
    private BigDecimal ticketPrice;
    private BigDecimal ccFee;
    private List<GateReportDto> reportData;
    private List<GateBankSummaryDto> bankSummary;
    private List<GateSalesSummaryDto> salesSummary;
    private GateTotalsDto totals;
}
