package com.kuleanasystems.assets.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetSearchDto {
    private AssetType assetType;
    
    // Common fields
    private UUID assetId; // cashBankId or ticketBookId
    private String status;
    private UUID operationId;
    private UUID shiftId;
    private UUID gateId;
    private String deviceId;
    private UUID processId;
    private String createdBy;
    private Instant createdAt;
    private String updatedBy;
    private Instant updatedAt;

    // CashBank specific fields
    private String bankName;
    private BigDecimal startingAmount;
    private BigDecimal officialCount;
    private Integer count100s;
    private Integer count50s;
    private Integer count20s;
    private Integer count10s;
    private Integer count5s;
    private Integer count1s;
    private Integer count1cs;
    private UUID parentCashBankId;

    // TicketBook specific fields
    private String scanCode;
    private Integer startNum;
    private Integer endNum;
    private Integer nextTicket;
    private Boolean isReusable;
    private UUID parentTicketBookId;
}