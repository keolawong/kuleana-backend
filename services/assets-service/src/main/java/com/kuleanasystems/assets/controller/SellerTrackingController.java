package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.dto.SellerPerformanceDto;
import com.kuleanasystems.assets.service.SellerTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/seller-tracking", "/seller-tracking"})
@RequiredArgsConstructor
public class SellerTrackingController {

    private final SellerTrackingService sellerTrackingService;

    @GetMapping("/performance")
    @PreAuthorize("hasAnyRole('ASSET-READ', 'asset-read', 'ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'VIEWER', 'viewer', 'PAGE_/SELLER-TRACKING', 'PAGE_/seller-tracking')")
    public ResponseEntity<List<SellerPerformanceDto>> getSellerPerformance(
            @RequestParam(required = false) UUID operationId,
            @RequestParam(required = false) UUID shiftId,
            @RequestParam(required = false) UUID gateId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "true") Boolean activeOperationsOnly) {

        List<SellerPerformanceDto> performance = sellerTrackingService.getSellerPerformance(operationId, shiftId, gateId, search, activeOperationsOnly);
        return ResponseEntity.ok(performance);
    }
}
