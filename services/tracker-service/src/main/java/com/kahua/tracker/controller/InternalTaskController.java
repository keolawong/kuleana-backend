package com.kahua.tracker.controller;

import com.kahua.tracker.entity.TrackingLandingZone;
import com.kahua.tracker.repository.TrackingLandingZoneRepository;
import com.kahua.tracker.service.CloudTasksService;
import com.kahua.tracker.service.LandingZoneProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/internal/tasks")
@RequiredArgsConstructor
@Slf4j
public class InternalTaskController {

    private final LandingZoneProcessor landingZoneProcessor;
    private final TrackingLandingZoneRepository landingZoneRepository;
    private final CloudTasksService cloudTasksService;

    @Value("${security.internal-task-api-key:default-internal-key-change-me}")
    private String internalApiKey;

    @PostMapping("/process-landing-zone/{id}")
    public ResponseEntity<Void> processLandingZone(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-Internal-Task-Key", required = false) String taskKey,
            @RequestHeader(value = "X-CloudTasks-TaskRetryCount", required = false, defaultValue = "0") int retryCount) {

        if (taskKey == null || !taskKey.equals(internalApiKey)) {
            log.warn("Unauthorized attempt to access internal task webhook for ID: {}", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            landingZoneProcessor.processLandingZoneRecord(id, retryCount);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed executing task for Landing Zone record [{}]", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/sweep-landing-zone")
    public ResponseEntity<Map<String, Object>> sweepLandingZone(
            @RequestHeader(value = "X-Internal-Task-Key", required = false) String taskKey) {

        if (taskKey == null || !taskKey.equals(internalApiKey)) {
            log.warn("Unauthorized attempt to access sweep-landing-zone webhook");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime landedThreshold = now.minusMinutes(5);
        OffsetDateTime processingThreshold = now.minusMinutes(15);

        List<TrackingLandingZone> staleRecords = landingZoneRepository.findStaleLandingZoneRecords(landedThreshold, processingThreshold);

        for (TrackingLandingZone record : staleRecords) {
            cloudTasksService.enqueueLandingZoneTask(record.getId());
        }

        log.info("Sweeper enqueued {} stale Landing Zone records for processing", staleRecords.size());
        return ResponseEntity.ok(Map.of("count", staleRecords.size()));
    }
}
