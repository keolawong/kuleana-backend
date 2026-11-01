package com.kahua.tracker.service;

import com.kahua.tracker.dto.flowtracking.FlowTrackingCreateRequest;
import com.kahua.tracker.dto.flowtracking.FlowTrackingUpdateRequest;
import com.kahua.tracker.entity.Application;
import com.kahua.tracker.entity.Flow;
import com.kahua.tracker.entity.FlowTracking;
import com.kahua.tracker.repository.ApplicationRepository;
import com.kahua.tracker.repository.FlowRepository;
import com.kahua.tracker.repository.FlowTrackingRepository;
import com.kahua.tracker.spec.FlowTrackingSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FlowTrackingService {

    private final FlowTrackingRepository flowTrackingRepository;
    private final FlowRepository flowRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional
    public FlowTracking createFlowTracking(FlowTrackingCreateRequest request) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        Application application = applicationRepository.findByName(request.getApplicationName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Application not found with name: " + request.getApplicationName()));

        Flow flow = flowRepository.findByFlowNameAndApplicationId(request.getFlowName(), application.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Flow not found with name: " + request.getFlowName() + " for application: " + request.getApplicationName()));

        FlowTracking flowTracking = FlowTracking.builder()
                .flowId(flow.getId())
                .externalId(request.getExternalId())
                .status(request.getStatus())
                .type(request.getType())
                .sessionId(request.getSessionId())
                .gateId(request.getGateId())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();
        return flowTrackingRepository.save(flowTracking);
    }

    @Transactional(readOnly = true)
    public List<FlowTracking> searchFlowTrackings(UUID flowId, UUID externalId, String status, String type, UUID sessionId, UUID gateId) {
        Specification<FlowTracking> spec = FlowTrackingSpecification.findByCriteria(flowId, externalId, status, type, sessionId, gateId);
        return flowTrackingRepository.findAll(spec);
    }

    @Transactional
    public Optional<FlowTracking> updateFlowTracking(UUID id, FlowTrackingUpdateRequest request) {
        Optional<FlowTracking> existingFlowTracking = flowTrackingRepository.findById(id);
        if (existingFlowTracking.isEmpty()) {
            return Optional.empty();
        }

        FlowTracking flowTrackingToUpdate = existingFlowTracking.get();
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        if (request.getApplicationName().isPresent() && request.getFlowName().isPresent()) {
            Application application = applicationRepository.findByName(request.getApplicationName().get())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Application not found with name: " + request.getApplicationName().get()));

            Flow flow = flowRepository.findByFlowNameAndApplicationId(request.getFlowName().get(), application.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Flow not found with name: " + request.getFlowName().get() + " for application: " + request.getApplicationName().get()));
            flowTrackingToUpdate.setFlowId(flow.getId());
        }

        request.getExternalId().ifPresent(flowTrackingToUpdate::setExternalId);
        request.getStatus().ifPresent(flowTrackingToUpdate::setStatus);
        request.getType().ifPresent(flowTrackingToUpdate::setType);
        request.getSessionId().ifPresent(flowTrackingToUpdate::setSessionId);
        request.getGateId().ifPresent(flowTrackingToUpdate::setGateId);
        flowTrackingToUpdate.setUpdatedBy(currentUser);

        return Optional.of(flowTrackingRepository.save(flowTrackingToUpdate));
    }
}
