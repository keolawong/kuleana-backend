package com.kahua.tracker.controller;

import com.kahua.tracker.dto.tracking.TrackingStatusRequest;
import com.kahua.tracker.dto.tracking.TrackingStatusResponse;
import com.kahua.tracker.service.TrackingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kahua.tracker.entity.TrackingLandingZone;
import com.kahua.tracker.repository.TrackingLandingZoneRepository;
import com.kahua.tracker.service.CloudTasksService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/tracking/status")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TrackingController {

    private final TrackingService trackingService;
    private final TrackingLandingZoneRepository landingZoneRepository;
    private final CloudTasksService cloudTasksService;
    private final ObjectMapper objectMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR', 'editor', 'ADMIN', 'admin', 'ASSET-WRITE', 'asset-write', 'PAGE_/DEPLOY', 'PAGE_/INVENTORY', 'PAGE_/RECONCILE')")
    public ResponseEntity<Void> submitTrackingStatus(@RequestBody TrackingStatusRequest request) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            TrackingLandingZone landingZone = TrackingLandingZone.builder()
                    .payload(objectMapper.valueToTree(request))
                    .originalUser(currentUser)
                    .status("LANDED")
                    .build();
            landingZoneRepository.save(landingZone);
            cloudTasksService.enqueueLandingZoneTask(landingZone.getId());
            return ResponseEntity.accepted().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('EDITOR', 'editor', 'ADMIN', 'admin', 'ASSET-WRITE', 'asset-write', 'PAGE_/DEPLOY', 'PAGE_/INVENTORY', 'PAGE_/RECONCILE')")
    public ResponseEntity<Void> submitBulkTrackingStatus(@RequestBody List<TrackingStatusRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            List<TrackingLandingZone> landingZones = new java.util.ArrayList<>(requests.size());
            for (TrackingStatusRequest req : requests) {
                landingZones.add(TrackingLandingZone.builder()
                        .payload(objectMapper.valueToTree(req))
                        .originalUser(currentUser)
                        .status("LANDED")
                        .build());
            }
            List<TrackingLandingZone> saved = landingZoneRepository.saveAll(landingZones);
            for (TrackingLandingZone record : saved) {
                cloudTasksService.enqueueLandingZoneTask(record.getId());
            }
            return ResponseEntity.accepted().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER', 'viewer', 'EDITOR', 'editor', 'ADMIN', 'admin', 'ASSET-READ', 'asset-read', 'ASSET-WRITE', 'asset-write', 'PAGE_/DEPLOY', 'PAGE_/INVENTORY', 'PAGE_/RECONCILE')")
    public ResponseEntity<List<TrackingStatusResponse>> searchTrackingStatus(
            @RequestParam(required = false) UUID flowId,
            @RequestParam(required = false) UUID externalId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID sessionId,
            @RequestParam(required = false) UUID gateId) {
        List<TrackingStatusResponse> responses = trackingService.searchTrackingStatus(flowId, externalId, status, type, sessionId, gateId);
        if (responses.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(responses);
    }
}
