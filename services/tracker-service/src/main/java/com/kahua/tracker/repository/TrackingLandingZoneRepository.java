package com.kahua.tracker.repository;

import com.kahua.tracker.entity.TrackingLandingZone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TrackingLandingZoneRepository extends JpaRepository<TrackingLandingZone, UUID> {
    Page<TrackingLandingZone> findByStatus(String status, Pageable pageable);
    List<TrackingLandingZone> findByStatus(String status);
    List<TrackingLandingZone> findAllByOrderByCreatedAtDesc();
    List<TrackingLandingZone> findByStatusOrderByCreatedAtDesc(String status);

    Page<TrackingLandingZone> findAllByCreatedAtGreaterThanEqual(OffsetDateTime since, Pageable pageable);
    Page<TrackingLandingZone> findByStatusAndCreatedAtGreaterThanEqual(String status, OffsetDateTime since, Pageable pageable);
    Page<TrackingLandingZone> findAllByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end, Pageable pageable);
    Page<TrackingLandingZone> findByStatusAndCreatedAtBetween(String status, OffsetDateTime start, OffsetDateTime end, Pageable pageable);
    Page<TrackingLandingZone> findAllByCreatedAtLessThanEqual(OffsetDateTime until, Pageable pageable);
    Page<TrackingLandingZone> findByStatusAndCreatedAtLessThanEqual(String status, OffsetDateTime until, Pageable pageable);

    @Query("SELECT t FROM TrackingLandingZone t WHERE (t.status = 'LANDED' AND t.createdAt < :landedThreshold) OR (t.status = 'PROCESSING' AND t.updatedAt < :processingThreshold)")
    List<TrackingLandingZone> findStaleLandingZoneRecords(
            @Param("landedThreshold") OffsetDateTime landedThreshold,
            @Param("processingThreshold") OffsetDateTime processingThreshold
    );
}
