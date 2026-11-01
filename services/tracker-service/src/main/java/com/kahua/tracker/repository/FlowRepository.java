package com.kahua.tracker.repository;

import com.kahua.tracker.entity.Flow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlowRepository extends JpaRepository<Flow, UUID>, JpaSpecificationExecutor<Flow> {
    Optional<Flow> findByFlowNameAndApplicationId(String flowName, UUID applicationId);
}
