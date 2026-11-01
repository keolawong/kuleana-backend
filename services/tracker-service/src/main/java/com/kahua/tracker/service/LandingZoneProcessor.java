package com.kahua.tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kahua.tracker.dto.tracking.TrackingStatusRequest;
import com.kahua.tracker.entity.TrackingLandingZone;
import com.kahua.tracker.repository.TrackingLandingZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LandingZoneProcessor {

    private final TrackingLandingZoneRepository landingZoneRepository;
    private final TrackingService trackingService;
    private final ObjectMapper objectMapper;
    private static final String INSTANCE_ID = UUID.randomUUID().toString().substring(0, 8);

    public void processLandingZoneRecord(UUID landingZoneId, int retryCount) {
        log.info("Processing Landing Zone record [{}] (Attempt {})", landingZoneId, retryCount + 1);

        TrackingLandingZone record = landingZoneRepository.findById(landingZoneId)
                .orElseThrow(() -> new IllegalArgumentException("Landing Zone record not found with ID: " + landingZoneId));

        if ("COMPLETED".equals(record.getStatus())) {
            log.info("Landing Zone record [{}] is already COMPLETED. Skipping.", landingZoneId);
            return;
        }

        String hostname = getHostname();

        try {
            // Claim the record and track attempt
            record.setStatus("PROCESSING");
            record.setClaimedBy(hostname);
            record.setRetryCount(retryCount);
            record.setErrorMessage(null);
            landingZoneRepository.saveAndFlush(record);

            // Parse and process
            TrackingStatusRequest request = objectMapper.treeToValue(record.getPayload(), TrackingStatusRequest.class);

            // Process async tracking status
            trackingService.processTrackingStatusAsync(request, record.getOriginalUser());

            record.setStatus("COMPLETED");
            record.setErrorMessage(null);
            landingZoneRepository.saveAndFlush(record);
            log.info("Successfully processed Landing Zone record [{}]", landingZoneId);
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.error("Error processing Landing Zone record [{}] on attempt {}", landingZoneId, retryCount + 1, e);

            record.setRetryCount(retryCount);
            record.setErrorMessage(errorMsg);

            if (retryCount >= 4) {
                // Final attempt (5th attempt) - mark as FAILED permanently and swallow exception so Cloud Tasks stops retrying
                log.warn("Marking Landing Zone record [{}] as FAILED permanently after 5 attempts.", landingZoneId);
                record.setStatus("FAILED");
                landingZoneRepository.saveAndFlush(record);
            } else {
                // For attempts 1..4: record errorMessage, retryCount, status = 'PROCESSING', throw RuntimeException for Cloud Tasks auto-retry
                record.setStatus("PROCESSING");
                landingZoneRepository.saveAndFlush(record);
                throw new RuntimeException("Transient failure processing Landing Zone record: " + landingZoneId, e);
            }
        }
    }

    private String getHostname() {
        String kService = System.getenv("K_SERVICE");
        String kRevision = System.getenv("K_REVISION");
        if (kService != null && kRevision != null) {
            return kService + "/" + kRevision + "-" + INSTANCE_ID;
        }
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + INSTANCE_ID;
        } catch (UnknownHostException e) {
            return "unknown-host-" + INSTANCE_ID;
        }
    }
}
