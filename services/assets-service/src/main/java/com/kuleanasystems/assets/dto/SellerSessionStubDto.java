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
public class SellerSessionStubDto {
    private UUID shiftId;
    private String shiftName;
    private String operationName;
    private UUID gateId;
    private String gateName;
    private String deviceId;
    private String sellerType; // "CASH" or "CREDIT"
    private Integer ticketsSold;
    private BigDecimal startingAmount;
    private BigDecimal expectedAmount;
    private BigDecimal collectedAmount; // officialCount for cash, creditReportTotal for credit
    private BigDecimal variance;        // collectedAmount - (startingAmount + expectedAmount) [Cash] or collectedAmount - expectedAmount [Credit]
    private String reconciledAt;
}
