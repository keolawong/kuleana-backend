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
public class CreditReportDto {
    private BigDecimal totalAmount;
    private UUID operationId;
    private UUID shiftId;
    private UUID gateId;
    private String deviceId;
    private UUID processId;
    private UUID personStickerId;
}