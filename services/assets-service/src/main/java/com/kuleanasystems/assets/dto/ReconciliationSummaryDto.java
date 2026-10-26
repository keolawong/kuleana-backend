package com.kuleanasystems.assets.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ReconciliationSummaryDto {
    private UUID operationId;
    private UUID shiftId;
    private String operationName;
    private String shiftName;
    private BigDecimal startingFloatTotal;
    private BigDecimal verifiedCashIntake;
    private BigDecimal creditCardBatchTotal;
    private BigDecimal grossMonetaryIntake;
    private BigDecimal netSalesTotal;
    private int totalBanksCount;
    private int reconciledBanksCount;
    private List<CashierIntakeDto> cashierBreakdowns;

    @Data
    @Builder
    public static class CashierIntakeDto {
        private String cashierName;
        private String gateName;
        private String deviceId;
        private List<String> bankIds;
        private BigDecimal startingFloat;
        private BigDecimal officialCount;
        private BigDecimal creditCardTotal;
        private BigDecimal netSales;
        private BigDecimal variance;
        private String status;
        private boolean deduplicatedEnvelope;
    }
}
