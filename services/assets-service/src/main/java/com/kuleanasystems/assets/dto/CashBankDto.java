package com.kuleanasystems.assets.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashBankDto {
    private UUID cashBankId;
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
    private String status;
    private UUID operationId;
    private UUID shiftId;
    private UUID gateId;
    private String deviceId;
    private UUID parentCashBankId;
    private UUID processId;
    private UUID personStickerId;
}