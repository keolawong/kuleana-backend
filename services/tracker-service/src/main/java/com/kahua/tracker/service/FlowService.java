package com.kahua.tracker.service;

import com.kahua.tracker.dto.flow.FlowCreateRequest;
import com.kahua.tracker.dto.flow.FlowUpdateRequest;
import com.kahua.tracker.entity.Flow;
import com.kahua.tracker.repository.FlowRepository;
import com.kahua.tracker.spec.FlowSpecification;
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
public class FlowService {

    private final FlowRepository flowRepository;

    @Transactional
    public Flow createFlow(FlowCreateRequest request) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Flow flow = Flow.builder()
                .flowName(request.getFlowName())
                .applicationId(request.getApplicationId())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();
        return flowRepository.save(flow);
    }

    @Transactional(readOnly = true)
    public List<Flow> searchFlows(String flowName, UUID applicationId, Boolean isActive) {
        Specification<Flow> spec = FlowSpecification.findByCriteria(flowName, applicationId, isActive);
        return flowRepository.findAll(spec);
    }

    @Transactional
    public Optional<Flow> updateFlow(UUID id, FlowUpdateRequest request) {
        Optional<Flow> existingFlow = flowRepository.findById(id);
        if (existingFlow.isEmpty()) {
            return Optional.empty();
        }

        Flow flowToUpdate = existingFlow.get();
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        request.getFlowName().ifPresent(flowToUpdate::setFlowName);
        request.getApplicationId().ifPresent(flowToUpdate::setApplicationId);
        request.getDescription().ifPresent(flowToUpdate::setDescription);
        request.getIsActive().ifPresent(flowToUpdate::setIsActive);
        flowToUpdate.setUpdatedBy(currentUser);

        return Optional.of(flowRepository.save(flowToUpdate));
    }
}
