package com.kahua.tracker.repository;

import com.kahua.tracker.entity.FlowTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlowTrackingRepository extends JpaRepository<FlowTracking, UUID>, JpaSpecificationExecutor<FlowTracking> {
    Optional<FlowTracking> findByExternalId(UUID externalId);
    Optional<FlowTracking> findByExternalIdAndParentIdIsNull(UUID externalId);
    java.util.List<FlowTracking> findByParentId(UUID parentId);
    java.util.List<FlowTracking> findByParentIdOrderByCreatedAtAsc(UUID parentId);
}
