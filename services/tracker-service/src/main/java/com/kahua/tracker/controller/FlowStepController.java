package com.kahua.tracker.controller;

import com.kahua.tracker.dto.flowstep.FlowStepCreateRequest;
import com.kahua.tracker.dto.flowstep.FlowStepResponse;
import com.kahua.tracker.dto.flowstep.FlowStepUpdateRequest;
import com.kahua.tracker.entity.FlowStep;
import com.kahua.tracker.service.FlowStepService;
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
@RequestMapping("/api/flow-steps")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class FlowStepController {

    private final FlowStepService flowStepService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<FlowStepResponse> createFlowStep(@RequestBody FlowStepCreateRequest request) {
        FlowStep savedFlowStep = flowStepService.createFlowStep(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedFlowStep.getId())
                .toUri();
        return ResponseEntity.created(location).body(mapToFlowStepResponse(savedFlowStep));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<FlowStepResponse> updateFlowStep(@PathVariable UUID id, @RequestBody FlowStepUpdateRequest request) {
        return flowStepService.updateFlowStep(id, request)
                .map(this::mapToFlowStepResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('VIEWER')")
    public ResponseEntity<List<FlowStepResponse>> searchFlowSteps(
            @RequestParam(required = false) UUID flowId,
            @RequestParam(required = false) String stepName,
            @RequestParam(required = false) Integer stepSeqNum) {
        List<FlowStep> flowSteps = flowStepService.searchFlowSteps(flowId, stepName, stepSeqNum);
        if (flowSteps.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        List<FlowStepResponse> response = flowSteps.stream()
                .map(this::mapToFlowStepResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    private FlowStepResponse mapToFlowStepResponse(FlowStep flowStep) {
        FlowStepResponse response = new FlowStepResponse();
        response.setId(flowStep.getId());
        response.setFlowId(flowStep.getFlowId());
        response.setStepName(flowStep.getStepName());
        response.setDescription(flowStep.getDescription());
        response.setStepSeqNum(flowStep.getStepSeqNum());
        response.setCreatedBy(flowStep.getCreatedBy());
        response.setCreatedAt(flowStep.getCreatedAt());
        response.setUpdatedBy(flowStep.getUpdatedBy());
        response.setUpdatedAt(flowStep.getUpdatedAt());
        return response;
    }
}
