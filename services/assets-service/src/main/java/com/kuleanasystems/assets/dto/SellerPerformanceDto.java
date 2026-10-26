package com.kuleanasystems.assets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerPerformanceDto {
    private String externalId;
    private String sellerName;
    private UUID personId;
    private String organization;
    private Integer totalSessions;
    private Integer balancedSessions;
    private Integer overSessions;
    private Integer underSessions;
    private BigDecimal netVariance;
    private BigDecimal totalAbsoluteDiscrepancy;
    private BigDecimal totalExpected;
    private BigDecimal totalCollected;
    private BigDecimal accuracyRate;
    private String performanceStatus; // "BALANCED", "OVER", "UNDER"
    @Builder.Default
    private List<SellerSessionStubDto> sessions = new ArrayList<>();
}
