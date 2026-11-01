package com.kahua.tracker.service;

import com.kahua.tracker.config.CacheConfig;
import com.kahua.tracker.dto.flowsteptracking.FlowStepTrackingResponse;
import com.kahua.tracker.dto.tracking.StepTrackingEntry;
import com.kahua.tracker.dto.tracking.TrackingStatusRequest;
import com.kahua.tracker.dto.tracking.TrackingStatusResponse;
import com.kahua.tracker.entity.Application;
import com.kahua.tracker.entity.Flow;
import com.kahua.tracker.entity.FlowStep;
import com.kahua.tracker.entity.FlowStepTracking;
import com.kahua.tracker.entity.FlowTracking;
import com.kahua.tracker.repository.ApplicationRepository;
import com.kahua.tracker.repository.FlowRepository;
import com.kahua.tracker.repository.FlowStepRepository;
import com.kahua.tracker.repository.FlowStepTrackingRepository;
import com.kahua.tracker.repository.FlowTrackingRepository;
import com.kahua.tracker.spec.FlowTrackingSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;
import java.time.OffsetDateTime;

@Service
public class TrackingService {

    public static final String PARENT_FLOW_NAME = "Asset-Lifecycle";
    public static final String CHILD_FLOW_NAME = "Asset-Action";
    public static final String LEGACY_FLOW_NAME = "Asset";

    private final ApplicationRepository applicationRepository;
    private final FlowRepository flowRepository;
    private final FlowStepRepository flowStepRepository;
    private final FlowTrackingRepository flowTrackingRepository;
    private final FlowStepTrackingRepository flowStepTrackingRepository;
    private final CacheManager cacheManager;

    @Autowired
    public TrackingService(
            ApplicationRepository applicationRepository,
            FlowRepository flowRepository,
            FlowStepRepository flowStepRepository,
            FlowTrackingRepository flowTrackingRepository,
            FlowStepTrackingRepository flowStepTrackingRepository,
            @Autowired(required = false) CacheManager cacheManager) {
        this.applicationRepository = applicationRepository;
        this.flowRepository = flowRepository;
        this.flowStepRepository = flowStepRepository;
        this.flowTrackingRepository = flowTrackingRepository;
        this.flowStepTrackingRepository = flowStepTrackingRepository;
        this.cacheManager = cacheManager;
    }

    public TrackingService(
            ApplicationRepository applicationRepository,
            FlowRepository flowRepository,
            FlowStepRepository flowStepRepository,
            FlowTrackingRepository flowTrackingRepository,
            FlowStepTrackingRepository flowStepTrackingRepository) {
        this(applicationRepository, flowRepository, flowStepRepository, flowTrackingRepository, flowStepTrackingRepository, null);
    }

    @Cacheable(value = CacheConfig.APPLICATION_CACHE, key = "#applicationName")
    public Application getApplicationByName(String applicationName) {
        if (cacheManager != null) {
            Cache cache = cacheManager.getCache(CacheConfig.APPLICATION_CACHE);
            if (cache != null) {
                return cache.get(applicationName, () -> applicationRepository.findByName(applicationName)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Application not found with name: " + applicationName)));
            }
        }
        return applicationRepository.findByName(applicationName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Application not found with name: " + applicationName));
    }

    @Cacheable(value = CacheConfig.FLOW_CACHE, key = "#flowName + ':' + #applicationId")
    public Flow getFlowByFlowNameAndApplicationId(String flowName, UUID applicationId, String applicationName) {
        String cacheKey = flowName + ":" + applicationId;
        if (cacheManager != null) {
            Cache cache = cacheManager.getCache(CacheConfig.FLOW_CACHE);
            if (cache != null) {
                return cache.get(cacheKey, () -> flowRepository.findByFlowNameAndApplicationId(flowName, applicationId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 
                                "Flow not found with name: " + flowName + " for application: " + applicationName)));
            }
        }
        return flowRepository.findByFlowNameAndApplicationId(flowName, applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 
                        "Flow not found with name: " + flowName + " for application: " + applicationName));
    }

    @Cacheable(value = CacheConfig.FLOW_CACHE, key = "#flowName + ':' + #applicationId")
    public Flow getFlowByFlowNameAndApplicationId(String flowName, UUID applicationId) {
        return getFlowByFlowNameAndApplicationId(flowName, applicationId, applicationId.toString());
    }

    @Cacheable(value = CacheConfig.FLOW_STEP_CACHE, key = "#stepName + ':' + #flowId")
    public Optional<FlowStep> getFlowStepByStepNameAndFlowId(String stepName, UUID flowId) {
        String cacheKey = stepName + ":" + flowId;
        if (cacheManager != null) {
            Cache cache = cacheManager.getCache(CacheConfig.FLOW_STEP_CACHE);
            if (cache != null) {
                return cache.get(cacheKey, () -> flowStepRepository.findByStepNameAndFlowId(stepName, flowId));
            }
        }
        return flowStepRepository.findByStepNameAndFlowId(stepName, flowId);
    }

    @Transactional
    public TrackingStatusResponse processTrackingStatus(TrackingStatusRequest request) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        return processTrackingStatusAsync(request, currentUser);
    }

    @Transactional
    public TrackingStatusResponse processTrackingStatusAsync(TrackingStatusRequest request, String currentUser) {

        // 1. Pre-Processing Fail-Fast Validation on Application
        Application application = getApplicationByName(request.getApplicationName());

        String requestedFlowName = request.getFlowName();
        if (requestedFlowName == null || requestedFlowName.isBlank()) {
            requestedFlowName = CHILD_FLOW_NAME;
        }

        // Normalize legacy flow name "Asset" to "Asset-Action"
        if (LEGACY_FLOW_NAME.equalsIgnoreCase(requestedFlowName)) {
            requestedFlowName = CHILD_FLOW_NAME;
        }

        // Check if this is an Asset-Action (Child) process with externalId
        if (CHILD_FLOW_NAME.equalsIgnoreCase(requestedFlowName) && request.getExternalId() != null) {
            return processChildAssetAction(request, application, currentUser);
        }

        // Check if this is an explicit Asset-Lifecycle (Parent) process
        if (PARENT_FLOW_NAME.equalsIgnoreCase(requestedFlowName) && request.getExternalId() != null) {
            return processParentAssetLifecycle(request, application, currentUser);
        }

        // Generic Flow Processing
        return processGenericFlow(request, application, requestedFlowName, currentUser);
    }

    private TrackingStatusResponse processChildAssetAction(TrackingStatusRequest request, Application application, String currentUser) {
        // 1. Resolve Child Flow & Steps
        Flow childFlow = getFlowByFlowNameAndApplicationId(CHILD_FLOW_NAME, application.getId(), application.getName());

        Map<String, FlowStep> resolvedSteps = resolveSteps(request.getSteps(), childFlow);

        // 2. Resolve or Create Parent Lifecycle Tracking
        Flow parentFlow = getFlowByFlowNameAndApplicationId(PARENT_FLOW_NAME, application.getId(), application.getName());

        FlowTracking parentTracking = flowTrackingRepository.findByExternalIdAndParentIdIsNull(request.getExternalId())
                .orElseGet(() -> createInitialParentTracking(request, parentFlow, currentUser));

        // 3. Update Parent Lifecycle Status if specified
        if (request.getParentStatus() != null && !request.getParentStatus().isBlank()) {
            String newParentStatus = request.getParentStatus().toUpperCase();
            if (!newParentStatus.equalsIgnoreCase(parentTracking.getStatus())) {
                parentTracking.setStatus(newParentStatus);
                parentTracking.setUpdatedBy(currentUser);
                parentTracking = flowTrackingRepository.save(parentTracking);

                // Record parent step transition if step exists
                Optional<FlowStep> parentStepOpt = getFlowStepByStepNameAndFlowId(newParentStatus, parentFlow.getId());
                if (parentStepOpt.isPresent()) {
                    FlowStepTracking parentStepTracking = FlowStepTracking.builder()
                            .flowTrackingId(parentTracking.getId())
                            .flowStepId(parentStepOpt.get().getId())
                            .status(newParentStatus)
                            .message("Parent lifecycle status transition to " + newParentStatus)
                            .createdAt(OffsetDateTime.now())
                            .createdBy(currentUser)
                            .build();
                    flowStepTrackingRepository.save(parentStepTracking);
                }
            }
        }

        // 4. Create Child FlowTracking Record
        FlowTracking childTracking = FlowTracking.builder()
                .parentId(parentTracking.getId())
                .flowId(childFlow.getId())
                .externalId(request.getExternalId())
                .status(request.getStatus())
                .type(request.getType() != null ? request.getType() : parentTracking.getType())
                .sessionId(request.getSessionId())
                .gateId(request.getGateId())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();
        childTracking = flowTrackingRepository.save(childTracking);

        // 5. Save Child Step Trackings
        List<FlowStepTracking> savedSteps = saveStepTrackings(request.getSteps(), childTracking.getId(), resolvedSteps, currentUser);

        return mapToTrackingStatusResponse(childTracking, savedSteps, CHILD_FLOW_NAME, null);
    }

    private TrackingStatusResponse processParentAssetLifecycle(TrackingStatusRequest request, Application application, String currentUser) {
        Flow parentFlow = getFlowByFlowNameAndApplicationId(PARENT_FLOW_NAME, application.getId(), application.getName());

        String parentStatus = request.getStatus() != null ? request.getStatus().toUpperCase() : "ACTIVE";
        FlowTracking parentTracking = flowTrackingRepository.findByExternalIdAndParentIdIsNull(request.getExternalId())
                .orElseGet(() -> {
                    FlowTracking newParent = FlowTracking.builder()
                            .flowId(parentFlow.getId())
                            .externalId(request.getExternalId())
                            .status(parentStatus)
                            .type(request.getType())
                            .createdBy(currentUser)
                            .updatedBy(currentUser)
                            .build();
                    return flowTrackingRepository.save(newParent);
                });

        parentTracking.setStatus(parentStatus);
        if (request.getType() != null) {
            parentTracking.setType(request.getType());
        }
        parentTracking.setUpdatedBy(currentUser);
        parentTracking = flowTrackingRepository.save(parentTracking);

        Map<String, FlowStep> resolvedSteps = resolveSteps(request.getSteps(), parentFlow);
        List<FlowStepTracking> savedSteps = saveStepTrackings(request.getSteps(), parentTracking.getId(), resolvedSteps, currentUser);

        return mapToTrackingStatusResponse(parentTracking, savedSteps, PARENT_FLOW_NAME, null);
    }

    private TrackingStatusResponse processGenericFlow(TrackingStatusRequest request, Application application, String flowName, String currentUser) {
        Flow flow = getFlowByFlowNameAndApplicationId(flowName, application.getId(), application.getName());

        Map<String, FlowStep> resolvedSteps = resolveSteps(request.getSteps(), flow);

        FlowTracking flowTracking;
        Optional<FlowTracking> existingTrackingOpt = request.getExternalId() != null 
                ? flowTrackingRepository.findByExternalId(request.getExternalId()) 
                : Optional.empty();

        if (existingTrackingOpt.isPresent()) {
            flowTracking = existingTrackingOpt.get();
            flowTracking.setFlowId(flow.getId());
            flowTracking.setStatus(request.getStatus());
            flowTracking.setType(request.getType());
            flowTracking.setSessionId(request.getSessionId());
            flowTracking.setGateId(request.getGateId());
            flowTracking.setUpdatedBy(currentUser);
        } else {
            flowTracking = FlowTracking.builder()
                    .flowId(flow.getId())
                    .externalId(request.getExternalId())
                    .status(request.getStatus())
                    .type(request.getType())
                    .sessionId(request.getSessionId())
                    .gateId(request.getGateId())
                    .createdBy(currentUser)
                    .updatedBy(currentUser)
                    .build();
        }

        flowTracking = flowTrackingRepository.save(flowTracking);
        List<FlowStepTracking> savedSteps = saveStepTrackings(request.getSteps(), flowTracking.getId(), resolvedSteps, currentUser);

        return mapToTrackingStatusResponse(flowTracking, savedSteps, flowName, null);
    }

    private FlowTracking createInitialParentTracking(TrackingStatusRequest request, Flow parentFlow, String currentUser) {
        String initialStatus = request.getParentStatus() != null ? request.getParentStatus().toUpperCase() : "ACTIVE";
        FlowTracking parentTracking = FlowTracking.builder()
                .flowId(parentFlow.getId())
                .externalId(request.getExternalId())
                .status(initialStatus)
                .type(request.getType())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();
        parentTracking = flowTrackingRepository.save(parentTracking);

        // Record initial step "ACTIVE" (or specified initial status)
        Optional<FlowStep> activeStepOpt = getFlowStepByStepNameAndFlowId(initialStatus, parentFlow.getId());
        if (activeStepOpt.isPresent()) {
            FlowStepTracking stepTracking = FlowStepTracking.builder()
                    .flowTrackingId(parentTracking.getId())
                    .flowStepId(activeStepOpt.get().getId())
                    .status(initialStatus)
                    .message("Asset initialized into parent lifecycle process")
                    .createdAt(OffsetDateTime.now())
                    .createdBy(currentUser)
                    .build();
            flowStepTrackingRepository.save(stepTracking);
        }

        return parentTracking;
    }

    private Map<String, FlowStep> resolveSteps(List<StepTrackingEntry> steps, Flow flow) {
        Map<String, FlowStep> resolved = new HashMap<>();
        if (steps != null) {
            for (StepTrackingEntry stepEntry : steps) {
                if (stepEntry.getStepName() != null && !resolved.containsKey(stepEntry.getStepName())) {
                    FlowStep flowStep = getFlowStepByStepNameAndFlowId(stepEntry.getStepName(), flow.getId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 
                                    "FlowStep not found with name: " + stepEntry.getStepName() + " in Flow: " + flow.getFlowName()));
                    resolved.put(stepEntry.getStepName(), flowStep);
                }
            }
        }
        return resolved;
    }

    private List<FlowStepTracking> saveStepTrackings(List<StepTrackingEntry> steps, UUID trackingId, Map<String, FlowStep> resolvedSteps, String currentUser) {
        if (steps == null || steps.isEmpty()) {
            return Collections.emptyList();
        }
        List<FlowStepTracking> stepTrackingsToSave = new ArrayList<>();
        for (StepTrackingEntry stepEntry : steps) {
            OffsetDateTime stepTimestamp = stepEntry.getUpdateTimestamp() != null
                    ? stepEntry.getUpdateTimestamp()
                    : OffsetDateTime.now();
            FlowStep flowStep = resolvedSteps.get(stepEntry.getStepName());
            FlowStepTracking stepTracking = FlowStepTracking.builder()
                    .flowTrackingId(trackingId)
                    .flowStepId(flowStep != null ? flowStep.getId() : null)
                    .status(stepEntry.getStatus())
                    .message(stepEntry.getMessage())
                    .amount(stepEntry.getAmount())
                    .count(stepEntry.getCount())
                    .createdAt(stepTimestamp)
                    .createdBy(currentUser)
                    .build();
            stepTrackingsToSave.add(stepTracking);
        }
        return flowStepTrackingRepository.saveAll(stepTrackingsToSave);
    }

    @Transactional(readOnly = true)
    public List<TrackingStatusResponse> searchTrackingStatus(UUID flowId, UUID externalId, String status, String type, UUID sessionId, UUID gateId) {
        // If searching specifically by externalId and no parentId was restricted, return the Parent process with its nested Child actions
        if (externalId != null) {
            Optional<FlowTracking> parentOpt = flowTrackingRepository.findByExternalIdAndParentIdIsNull(externalId);
            if (parentOpt.isPresent()) {
                FlowTracking parent = parentOpt.get();
                List<FlowTracking> children = flowTrackingRepository.findByParentIdOrderByCreatedAtAsc(parent.getId());

                List<UUID> allTrackingIds = new ArrayList<>();
                allTrackingIds.add(parent.getId());
                children.forEach(c -> allTrackingIds.add(c.getId()));

                List<FlowStepTracking> allSteps = flowStepTrackingRepository.findByFlowTrackingIdIn(allTrackingIds);
                Map<UUID, List<FlowStepTracking>> stepsByTrackingId = allSteps.stream()
                        .collect(Collectors.groupingBy(FlowStepTracking::getFlowTrackingId));

                List<FlowStepTracking> parentSteps = stepsByTrackingId.getOrDefault(parent.getId(), Collections.emptyList());
                List<TrackingStatusResponse> childResponses = children.stream().map(child -> {
                    List<FlowStepTracking> childSteps = stepsByTrackingId.getOrDefault(child.getId(), Collections.emptyList());
                    return mapToTrackingStatusResponse(child, childSteps, CHILD_FLOW_NAME, null);
                }).collect(Collectors.toList());

                TrackingStatusResponse parentResponse = mapToTrackingStatusResponse(parent, parentSteps, PARENT_FLOW_NAME, childResponses);
                return List.of(parentResponse);
            }
        }

        Specification<FlowTracking> spec = FlowTrackingSpecification.findByCriteria(flowId, externalId, status, type, sessionId, gateId);
        List<FlowTracking> trackings = flowTrackingRepository.findAll(spec);
        if (trackings.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> trackingIds = trackings.stream().map(FlowTracking::getId).collect(Collectors.toList());
        List<FlowStepTracking> allSteps = flowStepTrackingRepository.findByFlowTrackingIdIn(trackingIds);
        Map<UUID, List<FlowStepTracking>> stepsByTrackingId = allSteps.stream()
                .collect(Collectors.groupingBy(FlowStepTracking::getFlowTrackingId));

        return trackings.stream().map(tracking -> {
            List<FlowStepTracking> steps = stepsByTrackingId.getOrDefault(tracking.getId(), Collections.emptyList());
            return mapToTrackingStatusResponse(tracking, steps, null, null);
        }).collect(Collectors.toList());
    }

    private TrackingStatusResponse mapToTrackingStatusResponse(FlowTracking tracking, List<FlowStepTracking> steps, String flowName, List<TrackingStatusResponse> actions) {
        TrackingStatusResponse response = new TrackingStatusResponse();
        response.setId(tracking.getId());
        response.setParentId(tracking.getParentId());
        response.setFlowId(tracking.getFlowId());
        response.setFlowName(flowName);
        response.setExternalId(tracking.getExternalId());
        response.setStatus(tracking.getStatus());
        response.setType(tracking.getType());
        response.setSessionId(tracking.getSessionId());
        response.setGateId(tracking.getGateId());
        response.setCreatedBy(tracking.getCreatedBy());
        response.setCreatedAt(tracking.getCreatedAt());
        response.setUpdatedBy(tracking.getUpdatedBy());
        response.setUpdatedAt(tracking.getUpdatedAt());

        if (steps != null) {
            List<FlowStepTrackingResponse> stepResponses = steps.stream().map(this::mapToFlowStepTrackingResponse).collect(Collectors.toList());
            response.setSteps(stepResponses);
        } else {
            response.setSteps(Collections.emptyList());
        }

        response.setActions(actions != null ? actions : Collections.emptyList());

        return response;
    }

    private FlowStepTrackingResponse mapToFlowStepTrackingResponse(FlowStepTracking step) {
        FlowStepTrackingResponse response = new FlowStepTrackingResponse();
        response.setId(step.getId());
        response.setFlowTrackingId(step.getFlowTrackingId());
        response.setFlowStepId(step.getFlowStepId());
        response.setStatus(step.getStatus());
        response.setMessage(step.getMessage());
        response.setAmount(step.getAmount());
        response.setCount(step.getCount());
        response.setCreatedBy(step.getCreatedBy());
        response.setCreatedAt(step.getCreatedAt());
        return response;
    }
}
