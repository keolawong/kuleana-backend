package com.kuleanasystems.assets.dto.report;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationReportDto {
    private UUID operationId;
    private String operationName;
    private BigDecimal ticketPrice;
    private BigDecimal ccFee;
    private List<GateHeaderDto> gates;
    private Map<String, List<OperationSectionRowDto>> sections;
    private Map<String, Map<String, OperationTotalsDto>> sectionTotals;
}
