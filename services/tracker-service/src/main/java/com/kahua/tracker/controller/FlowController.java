package com.kahua.tracker.controller;

import com.kahua.tracker.dto.flow.FlowCreateRequest;
import com.kahua.tracker.dto.flow.FlowResponse;
import com.kahua.tracker.dto.flow.FlowUpdateRequest;
import com.kahua.tracker.entity.Flow;
import com.kahua.tracker.service.FlowService;
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
@RequestMapping("/api/flows")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class FlowController {

    private final FlowService flowService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<FlowResponse> createFlow(@RequestBody FlowCreateRequest request) {
        Flow savedFlow = flowService.createFlow(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedFlow.getId())
                .toUri();
        return ResponseEntity.created(location).body(mapToFlowResponse(savedFlow));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<FlowResponse> updateFlow(@PathVariable UUID id, @RequestBody FlowUpdateRequest request) {
        return flowService.updateFlow(id, request)
                .map(this::mapToFlowResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('VIEWER')")
    public ResponseEntity<List<FlowResponse>> searchFlows(
            @RequestParam(required = false) String flowName,
            @RequestParam(required = false) UUID applicationId,
            @RequestParam(required = false) Boolean isActive) {
        List<Flow> flows = flowService.searchFlows(flowName, applicationId, isActive);
        if (flows.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        List<FlowResponse> response = flows.stream()
                .map(this::mapToFlowResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    private FlowResponse mapToFlowResponse(Flow flow) {
        FlowResponse response = new FlowResponse();
        response.setId(flow.getId());
        response.setFlowName(flow.getFlowName());
        response.setApplicationId(flow.getApplicationId());
        response.setDescription(flow.getDescription());
        response.setIsActive(flow.getIsActive());
        response.setCreatedBy(flow.getCreatedBy());
        response.setCreatedAt(flow.getCreatedAt());
        response.setUpdatedBy(flow.getUpdatedBy());
        response.setUpdatedAt(flow.getUpdatedAt());
        return response;
    }
}
