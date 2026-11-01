package com.kahua.tracker.controller;

import com.kahua.tracker.dto.tracking.LandingZoneResponse;
import com.kahua.tracker.dto.tracking.ReprocessBulkRequest;
import com.kahua.tracker.entity.TrackingLandingZone;
import com.kahua.tracker.repository.TrackingLandingZoneRepository;
import com.kahua.tracker.service.CloudTasksService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/tracking/landing-zone")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class LandingZoneController {

    private final TrackingLandingZoneRepository landingZoneRepository;
    private final CloudTasksService cloudTasksService;

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER', 'viewer', 'EDITOR', 'editor', 'ADMIN', 'admin', 'ASSET-READ', 'asset-read', 'PAGE_/LANDING-ZONE')")
    public ResponseEntity<List<LandingZoneResponse>> getLandingZoneRecords(@RequestParam(required = false) String status) {
        List<TrackingLandingZone> records;
        if (status != null && !status.trim().isEmpty()) {
            records = landingZoneRepository.findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase());
        } else {
            records = landingZoneRepository.findAllByOrderByCreatedAtDesc();
        }

        List<LandingZoneResponse> responses = records.stream()
                .map(this::mapToResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('VIEWER', 'viewer', 'EDITOR', 'editor', 'ADMIN', 'admin', 'ASSET-READ', 'asset-read', 'PAGE_/LANDING-ZONE')")
    public SseEmitter streamLandingZoneRecords(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @RequestParam(required = false) String token,
            HttpServletRequest request
    ) {
        String resolvedToken = extractToken(token, request);
        log.debug("Initiating Landing Zone SSE stream: status={}, startDate={}, endDate={}, hasToken={}",
                status, startDate, endDate, resolvedToken != null);

        SseEmitter emitter = new SseEmitter(180_000L);
        AtomicBoolean isCompleted = new AtomicBoolean(false);

        emitter.onCompletion(() -> {
            isCompleted.set(true);
            log.info("Landing Zone SSE stream completed");
        });
        emitter.onTimeout(() -> {
            isCompleted.set(true);
            log.warn("Landing Zone SSE stream timed out");
            emitter.complete();
        });
        emitter.onError(ex -> {
            isCompleted.set(true);
            log.warn("Landing Zone SSE stream error: {}", ex.getMessage());
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("OPEN")
                    .data(Map.of("status", "connected")));
        } catch (IOException e) {
            log.error("Failed to send OPEN event to Landing Zone SSE stream", e);
            emitter.completeWithError(e);
            return emitter;
        }

        CompletableFuture.runAsync(() -> {
            try {
                int totalRecords = 0;
                long offset = 0;
                int limit = 20;

                while (!isCompleted.get()) {
                    Pageable pageable = (offset == 0 && limit == 20)
                            ? PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
                            : new OffsetLimitPageRequest(offset, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

                    Page<TrackingLandingZone> chunk = queryRecords(status, startDate, endDate, pageable);
                    if (chunk == null || chunk.isEmpty()) {
                        break;
                    }

                    List<LandingZoneResponse> chunkResponses = chunk.getContent().stream()
                            .map(this::mapToResponse)
                            .toList();

                    if (!chunkResponses.isEmpty()) {
                        totalRecords += chunkResponses.size();
                        emitter.send(SseEmitter.event().name("CHUNK").data(chunkResponses));
                    }

                    if (chunkResponses.size() < limit || !chunk.hasNext()) {
                        break;
                    }

                    offset += limit;
                    limit = 50;
                }

                if (!isCompleted.get()) {
                    emitter.send(SseEmitter.event()
                            .name("STREAM_COMPLETE")
                            .data(Map.of("totalCount", totalRecords, "completedAt", OffsetDateTime.now().toString())));
                    emitter.complete();
                }
            } catch (Exception e) {
                log.error("Error streaming Landing Zone records", e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {
                }
            }
        });

        return emitter;
    }

    private String extractToken(String token, HttpServletRequest request) {
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        if (request != null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7).trim();
            }
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("jwt".equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
        }
        return null;
    }

    private Page<TrackingLandingZone> queryRecords(String status, OffsetDateTime startDate, OffsetDateTime endDate, Pageable pageable) {
        boolean hasStatus = (status != null && !status.trim().isEmpty());
        String cleanStatus = hasStatus ? status.trim().toUpperCase() : null;
        boolean hasStart = (startDate != null);
        boolean hasEnd = (endDate != null);

        if (cleanStatus != null) {
            if (hasStart && hasEnd) {
                return landingZoneRepository.findByStatusAndCreatedAtBetween(cleanStatus, startDate, endDate, pageable);
            } else if (hasStart) {
                return landingZoneRepository.findByStatusAndCreatedAtGreaterThanEqual(cleanStatus, startDate, pageable);
            } else if (hasEnd) {
                return landingZoneRepository.findByStatusAndCreatedAtLessThanEqual(cleanStatus, endDate, pageable);
            } else {
                return landingZoneRepository.findByStatus(cleanStatus, pageable);
            }
        } else {
            if (hasStart && hasEnd) {
                return landingZoneRepository.findAllByCreatedAtBetween(startDate, endDate, pageable);
            } else if (hasStart) {
                return landingZoneRepository.findAllByCreatedAtGreaterThanEqual(startDate, pageable);
            } else if (hasEnd) {
                return landingZoneRepository.findAllByCreatedAtLessThanEqual(endDate, pageable);
            } else {
                return landingZoneRepository.findAll(pageable);
            }
        }
    }

    private LandingZoneResponse mapToResponse(TrackingLandingZone r) {
        return LandingZoneResponse.builder()
                .id(r.getId())
                .payload(r.getPayload())
                .originalUser(r.getOriginalUser())
                .status(r.getStatus())
                .claimedBy(r.getClaimedBy())
                .retryCount(r.getRetryCount())
                .errorMessage(r.getErrorMessage())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    @PostMapping("/{id}/reprocess")
    @PreAuthorize("hasAnyRole('EDITOR', 'editor', 'ADMIN', 'admin', 'ASSET-WRITE', 'asset-write', 'PAGE_/LANDING-ZONE')")
    public ResponseEntity<Void> reprocessRecord(@PathVariable UUID id) {
        TrackingLandingZone record = landingZoneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Record not found with ID: " + id));

        record.setStatus("LANDED");
        record.setRetryCount(0);
        record.setErrorMessage(null);
        landingZoneRepository.save(record);

        cloudTasksService.enqueueLandingZoneTask(record.getId());
        log.info("Manually triggered reprocessing for Landing Zone record [{}]", id);

        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reprocess-bulk")
    @PreAuthorize("hasAnyRole('EDITOR', 'editor', 'ADMIN', 'admin', 'ASSET-WRITE', 'asset-write', 'PAGE_/LANDING-ZONE')")
    public ResponseEntity<Map<String, Object>> reprocessBulk(
            @RequestParam(required = false) String status,
            @RequestBody(required = false) ReprocessBulkRequest request) {

        String effectiveStatus = (request != null && request.getStatus() != null && !request.getStatus().isBlank())
                ? request.getStatus()
                : status;

        List<UUID> recordIds = (request != null && request.getRecordIds() != null)
                ? request.getRecordIds()
                : null;

        List<TrackingLandingZone> records;
        if (recordIds != null && !recordIds.isEmpty()) {
            records = landingZoneRepository.findAllById(recordIds);
        } else if (effectiveStatus != null && !effectiveStatus.isBlank()) {
            records = landingZoneRepository.findByStatus(effectiveStatus.trim().toUpperCase());
        } else {
            records = List.of();
        }

        for (TrackingLandingZone record : records) {
            record.setStatus("LANDED");
            record.setRetryCount(0);
            record.setErrorMessage(null);
            landingZoneRepository.save(record);
            cloudTasksService.enqueueLandingZoneTask(record.getId());
        }

        log.info("Bulk reprocessed {} Landing Zone records", records.size());
        return ResponseEntity.accepted().body(Map.of("count", records.size()));
    }

    @PostMapping("/reprocess-failed")
    @PreAuthorize("hasAnyRole('EDITOR', 'editor', 'ADMIN', 'admin', 'ASSET-WRITE', 'asset-write', 'PAGE_/LANDING-ZONE')")
    public ResponseEntity<Void> reprocessAllFailed() {
        reprocessBulk("FAILED", null);
        return ResponseEntity.accepted().build();
    }

    public static class OffsetLimitPageRequest implements Pageable {
        private final long offset;
        private final int limit;
        private final Sort sort;

        public OffsetLimitPageRequest(long offset, int limit, Sort sort) {
            if (offset < 0) {
                throw new IllegalArgumentException("Offset index must not be less than zero");
            }
            if (limit <= 0) {
                throw new IllegalArgumentException("Limit must be greater than zero");
            }
            this.offset = offset;
            this.limit = limit;
            this.sort = (sort == null) ? Sort.unsorted() : sort;
        }

        @Override
        public int getPageNumber() {
            return (int) (offset / limit);
        }

        @Override
        public int getPageSize() {
            return limit;
        }

        @Override
        public long getOffset() {
            return offset;
        }

        @Override
        public Sort getSort() {
            return sort;
        }

        @Override
        public Pageable next() {
            return new OffsetLimitPageRequest(offset + limit, limit, sort);
        }

        @Override
        public Pageable previousOrFirst() {
            return hasPrevious() ? new OffsetLimitPageRequest(Math.max(0, offset - limit), limit, sort) : this;
        }

        @Override
        public Pageable first() {
            return new OffsetLimitPageRequest(0, limit, sort);
        }

        @Override
        public Pageable withPage(int pageNumber) {
            return new OffsetLimitPageRequest((long) pageNumber * limit, limit, sort);
        }

        @Override
        public boolean hasPrevious() {
            return offset > 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pageable pageable)) return false;
            return offset == pageable.getOffset() && limit == pageable.getPageSize() && Objects.equals(sort, pageable.getSort());
        }

        @Override
        public int hashCode() {
            return Objects.hash(offset, limit, sort);
        }
    }
}
