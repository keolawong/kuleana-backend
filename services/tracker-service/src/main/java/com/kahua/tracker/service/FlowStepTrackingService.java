package com.kahua.tracker.service;

import com.kahua.tracker.dto.flowsteptracking.FlowStepTrackingCreateRequest;
import com.kahua.tracker.dto.flowsteptracking.FlowStepTrackingUpdateRequest;
import com.kahua.tracker.entity.FlowStep;
import com.kahua.tracker.entity.FlowStepTracking;
import com.kahua.tracker.repository.FlowStepRepository;
import com.kahua.tracker.repository.FlowStepTrackingRepository;
import com.kahua.tracker.spec.FlowStepTrackingSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FlowStepTrackingService {

    private final FlowStepTrackingRepository flowStepTrackingRepository;
    private final FlowStepRepository flowStepRepository;

    @Transactional
    public FlowStepTracking createFlowStepTracking(FlowStepTrackingCreateRequest request) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        
        FlowStep flowStep = flowStepRepository.findByStepName(request.getFlowStepName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "FlowStep not found with name: " + request.getFlowStepName()));

        FlowStepTracking flowStepTracking = FlowStepTracking.builder()
                .flowTrackingId(request.getFlowTrackingId())
                .flowStepId(flowStep.getId())
                .status(request.getStatus())
                .message(request.getMessage())
                .amount(request.getAmount())
                .count(request.getCount())
                .createdBy(currentUser)
                // FlowStepTracking does not have updatedBy
                .build();
        return flowStepTrackingRepository.save(flowStepTracking);
    }

    @Transactional(readOnly = true)
    public List<FlowStepTracking> searchFlowStepTrackings(UUID flowTrackingId, UUID flowStepId, String status, BigDecimal amount, Integer count) {
        Specification<FlowStepTracking> spec = FlowStepTrackingSpecification.findByCriteria(flowTrackingId, flowStepId, status, amount, count);
        return flowStepTrackingRepository.findAll(spec);
    }

    @Transactional
    public Optional<FlowStepTracking> updateFlowStepTracking(UUID id, FlowStepTrackingUpdateRequest request) {
        Optional<FlowStepTracking> existingFlowStepTracking = flowStepTrackingRepository.findById(id);
        if (existingFlowStepTracking.isEmpty()) {
            return Optional.empty();
        }

        FlowStepTracking flowStepTrackingToUpdate = existingFlowStepTracking.get();

        if (request.getFlowStepName().isPresent()) {
            FlowStep flowStep = flowStepRepository.findByStepName(request.getFlowStepName().get())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "FlowStep not found with name: " + request.getFlowStepName().get()));
            flowStepTrackingToUpdate.setFlowStepId(flowStep.getId());
        }

        request.getFlowTrackingId().ifPresent(flowStepTrackingToUpdate::setFlowTrackingId);
        request.getStatus().ifPresent(flowStepTrackingToUpdate::setStatus);
        request.getMessage().ifPresent(flowStepTrackingToUpdate::setMessage);
        request.getAmount().ifPresent(flowStepTrackingToUpdate::setAmount);
        request.getCount().ifPresent(flowStepTrackingToUpdate::setCount);
        // FlowStepTracking does not have updatedBy

        return Optional.of(flowStepTrackingRepository.save(flowStepTrackingToUpdate));
    }
}
