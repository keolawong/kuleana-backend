package com.kahua.tracker.controller;

import com.kahua.tracker.dto.flowtracking.FlowTrackingCreateRequest;
import com.kahua.tracker.dto.flowtracking.FlowTrackingResponse;
import com.kahua.tracker.dto.flowtracking.FlowTrackingUpdateRequest;
import com.kahua.tracker.entity.FlowTracking;
import com.kahua.tracker.service.FlowTrackingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/flow-trackings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class FlowTrackingController {

    private final FlowTrackingService flowTrackingService;

    @PostMapping
    @PreAuthorize("hasRole('EDITOR')")
    public ResponseEntity<FlowTrackingResponse> createFlowTracking(@RequestBody FlowTrackingCreateRequest request) {
        FlowTracking savedFlowTracking = flowTrackingService.createFlowTracking(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedFlowTracking.getId())
                .toUri();
        return ResponseEntity.created(location).body(mapToFlowTrackingResponse(savedFlowTracking));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('EDITOR')")
    public ResponseEntity<FlowTrackingResponse> updateFlowTracking(@PathVariable UUID id, @RequestBody FlowTrackingUpdateRequest request) {
        return flowTrackingService.updateFlowTracking(id, request)
                .map(this::mapToFlowTrackingResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('VIEWER')")
    public ResponseEntity<List<FlowTrackingResponse>> searchFlowTrackings(
            @RequestParam(required = false) UUID flowId,
            @RequestParam(required = false) UUID externalId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID sessionId,
            @RequestParam(required = false) UUID gateId) {
        List<FlowTracking> flowTrackings = flowTrackingService.searchFlowTrackings(flowId, externalId, status, type, sessionId, gateId);
        if (flowTrackings.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        List<FlowTrackingResponse> response = flowTrackings.stream()
                .map(this::mapToFlowTrackingResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    private FlowTrackingResponse mapToFlowTrackingResponse(FlowTracking flowTracking) {
        FlowTrackingResponse response = new FlowTrackingResponse();
        response.setId(flowTracking.getId());
        response.setFlowId(flowTracking.getFlowId());
        response.setExternalId(flowTracking.getExternalId());
        response.setStatus(flowTracking.getStatus());
        response.setType(flowTracking.getType());
        response.setSessionId(flowTracking.getSessionId());
        response.setGateId(flowTracking.getGateId());
        response.setCreatedBy(flowTracking.getCreatedBy());
        response.setCreatedAt(flowTracking.getCreatedAt());
        response.setUpdatedBy(flowTracking.getUpdatedBy());
        response.setUpdatedAt(flowTracking.getUpdatedAt());
        return response;
    }
}
