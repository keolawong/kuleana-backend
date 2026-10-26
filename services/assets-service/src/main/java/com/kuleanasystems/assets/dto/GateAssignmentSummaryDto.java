package com.kuleanasystems.assets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GateAssignmentSummaryDto {
    private int totalWorkers;
    private int totalNeeded;
    private int totalAssigned;
    private int solitaryCount;
    private int unassignedCount;

    // Separated Pool Totals
    private int totalSellers;
    private int sellersNeeded;
    private int sellersAssigned;

    private int totalScanners;
    private int scannersNeeded;
    private int scannersAssigned;

    private List<GateBreakdownDto> gateBreakdowns;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GateBreakdownDto {
        private UUID gateId;
        private String gateName;
        private int displayOrder;
        private int priority;
        private int sellersMin;
        private int sellersHappy;
        private int sellersNeeded;
        private int sellersAssigned;
        private int scannersMin;
        private int scannersHappy;
        private int scannersNeeded;
        private int scannersAssigned;
        private int flexNeeded;
        private int flexAssigned;
        private int totalNeeded;
        private int totalAssigned;
        private String status; // "STAFFED", "SHORT", "SURPLUS"
        private String milestoneStatus; // "BELOW_MIN", "MIN_MET", "HAPPY_MET", "PLANNED_MET"
    }
}
