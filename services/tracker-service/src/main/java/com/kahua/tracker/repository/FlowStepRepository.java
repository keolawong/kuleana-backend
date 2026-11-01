package com.kahua.tracker.repository;

import com.kahua.tracker.entity.FlowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlowStepRepository extends JpaRepository<FlowStep, UUID>, JpaSpecificationExecutor<FlowStep> {
    Optional<FlowStep> findByStepName(String stepName);
    Optional<FlowStep> findByStepNameAndFlowId(String stepName, UUID flowId);
}