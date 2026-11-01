package com.kahua.tracker.service;

import com.kahua.tracker.dto.flowstep.FlowStepCreateRequest;
import com.kahua.tracker.dto.flowstep.FlowStepUpdateRequest;
import com.kahua.tracker.entity.FlowStep;
import com.kahua.tracker.repository.FlowStepRepository;
import com.kahua.tracker.spec.FlowStepSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FlowStepService {

    private final FlowStepRepository flowStepRepository;

    @Transactional
    public FlowStep createFlowStep(FlowStepCreateRequest request) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        FlowStep flowStep = FlowStep.builder()
                .flowId(request.getFlowId())
                .stepName(request.getStepName())
                .description(request.getDescription())
                .stepSeqNum(request.getStepSeqNum())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();
        return flowStepRepository.save(flowStep);
    }

    @Transactional(readOnly = true)
    public List<FlowStep> searchFlowSteps(UUID flowId, String stepName, Integer stepSeqNum) {
        Specification<FlowStep> spec = FlowStepSpecification.findByCriteria(flowId, stepName, stepSeqNum);
        return flowStepRepository.findAll(spec);
    }

    @Transactional
    public Optional<FlowStep> updateFlowStep(UUID id, FlowStepUpdateRequest request) {
        Optional<FlowStep> existingFlowStep = flowStepRepository.findById(id);
        if (existingFlowStep.isEmpty()) {
            return Optional.empty();
        }

        FlowStep flowStepToUpdate = existingFlowStep.get();
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        request.getFlowId().ifPresent(flowStepToUpdate::setFlowId);
        request.getStepName().ifPresent(flowStepToUpdate::setStepName);
        request.getDescription().ifPresent(flowStepToUpdate::setDescription);
        request.getStepSeqNum().ifPresent(flowStepToUpdate::setStepSeqNum);
        flowStepToUpdate.setUpdatedBy(currentUser);

        return Optional.of(flowStepRepository.save(flowStepToUpdate));
    }
}
