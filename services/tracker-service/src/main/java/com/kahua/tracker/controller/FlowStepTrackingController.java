package com.kahua.tracker.controller;

import com.kahua.tracker.dto.flowsteptracking.FlowStepTrackingCreateRequest;
import com.kahua.tracker.dto.flowsteptracking.FlowStepTrackingResponse;
import com.kahua.tracker.dto.flowsteptracking.FlowStepTrackingUpdateRequest;
import com.kahua.tracker.entity.FlowStepTracking;
import com.kahua.tracker.service.FlowStepTrackingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/flow-step-trackings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class FlowStepTrackingController {

    private final FlowStepTrackingService flowStepTrackingService;

    @PostMapping
    @PreAuthorize("hasRole('EDITOR')")
    public ResponseEntity<FlowStepTrackingResponse> createFlowStepTracking(@RequestBody FlowStepTrackingCreateRequest request) {
        FlowStepTracking savedFlowStepTracking = flowStepTrackingService.createFlowStepTracking(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedFlowStepTracking.getId())
                .toUri();
        return ResponseEntity.created(location).body(mapToFlowStepTrackingResponse(savedFlowStepTracking));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('EDITOR')")
    public ResponseEntity<FlowStepTrackingResponse> updateFlowStepTracking(@PathVariable UUID id, @RequestBody FlowStepTrackingUpdateRequest request) {
        return flowStepTrackingService.updateFlowStepTracking(id, request)
                .map(this::mapToFlowStepTrackingResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('VIEWER')")
    public ResponseEntity<List<FlowStepTrackingResponse>> searchFlowStepTrackings(
            @RequestParam(required = false) UUID flowTrackingId,
            @RequestParam(required = false) UUID flowStepId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) Integer count) {
        List<FlowStepTracking> flowStepTrackings = flowStepTrackingService.searchFlowStepTrackings(flowTrackingId, flowStepId, status, amount, count);
        if (flowStepTrackings.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        List<FlowStepTrackingResponse> response = flowStepTrackings.stream()
                .map(this::mapToFlowStepTrackingResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    private FlowStepTrackingResponse mapToFlowStepTrackingResponse(FlowStepTracking flowStepTracking) {
        FlowStepTrackingResponse response = new FlowStepTrackingResponse();
        response.setId(flowStepTracking.getId());
        response.setFlowTrackingId(flowStepTracking.getFlowTrackingId());
        response.setFlowStepId(flowStepTracking.getFlowStepId());
        response.setStatus(flowStepTracking.getStatus());
        response.setMessage(flowStepTracking.getMessage());
        response.setAmount(flowStepTracking.getAmount());
        response.setCount(flowStepTracking.getCount());
        response.setCreatedBy(flowStepTracking.getCreatedBy());
        response.setCreatedAt(flowStepTracking.getCreatedAt());
        return response;
    }
}
