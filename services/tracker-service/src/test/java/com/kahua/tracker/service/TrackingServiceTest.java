package com.kahua.tracker.service;

import com.kahua.tracker.config.CacheConfig;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrackingServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private FlowRepository flowRepository;
    @Mock private FlowStepRepository flowStepRepository;
    @Mock private FlowTrackingRepository flowTrackingRepository;
    @Mock private FlowStepTrackingRepository flowStepTrackingRepository;

    @InjectMocks
    private TrackingService trackingService;

    @BeforeEach
    void setUp() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void processTrackingStatus_genericFlow_shouldUpsertAndReturnResponse() {
        // Arrange
        TrackingStatusRequest request = new TrackingStatusRequest();
        request.setApplicationName("MyApp");
        request.setFlowName("CustomFlow");
        request.setExternalId(UUID.randomUUID());
        request.setStatus("IN_PROGRESS");
        request.setType("STANDARD");

        StepTrackingEntry stepEntry = new StepTrackingEntry();
        stepEntry.setStepName("Step1");
        stepEntry.setStatus("COMPLETED");
        request.setSteps(List.of(stepEntry));

        Application app = new Application();
        app.setId(UUID.randomUUID());
        when(applicationRepository.findByName("MyApp")).thenReturn(Optional.of(app));

        Flow flow = new Flow();
        flow.setId(UUID.randomUUID());
        flow.setFlowName("CustomFlow");
        when(flowRepository.findByFlowNameAndApplicationId("CustomFlow", app.getId())).thenReturn(Optional.of(flow));

        FlowStep step = new FlowStep();
        step.setId(UUID.randomUUID());
        when(flowStepRepository.findByStepNameAndFlowId("Step1", flow.getId())).thenReturn(Optional.of(step));

        when(flowTrackingRepository.findByExternalId(request.getExternalId())).thenReturn(Optional.empty());

        FlowTracking savedTracking = new FlowTracking();
        savedTracking.setId(UUID.randomUUID());
        savedTracking.setFlowId(flow.getId());
        when(flowTrackingRepository.save(any(FlowTracking.class))).thenReturn(savedTracking);

        when(flowStepTrackingRepository.saveAll(anyList())).thenReturn(List.of());

        // Act
        TrackingStatusResponse result = trackingService.processTrackingStatus(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(savedTracking.getId());
        verify(flowTrackingRepository, times(1)).save(any(FlowTracking.class));
        verify(flowStepTrackingRepository, times(1)).saveAll(anyList());
    }

    @Test
    void processTrackingStatus_childAssetAction_shouldAutoCreateParentAndLinkChild() {
        // Arrange
        UUID externalId = UUID.randomUUID();
        TrackingStatusRequest request = new TrackingStatusRequest();
        request.setApplicationName("Kahua Asset");
        request.setFlowName("Asset-Action");
        request.setExternalId(externalId);
        request.setStatus("Deployed");
        request.setType("TICKET_BOOK");

        StepTrackingEntry stepEntry = new StepTrackingEntry();
        stepEntry.setStepName("ASSIGNMENT");
        stepEntry.setStatus("Assigned");
        stepEntry.setCount(50);
        request.setSteps(List.of(stepEntry));

        Application app = new Application();
        app.setId(UUID.randomUUID());
        app.setName("Kahua Asset");
        when(applicationRepository.findByName("Kahua Asset")).thenReturn(Optional.of(app));

        Flow parentFlow = new Flow();
        parentFlow.setId(UUID.randomUUID());
        parentFlow.setFlowName("Asset-Lifecycle");
        when(flowRepository.findByFlowNameAndApplicationId("Asset-Lifecycle", app.getId())).thenReturn(Optional.of(parentFlow));

        Flow childFlow = new Flow();
        childFlow.setId(UUID.randomUUID());
        childFlow.setFlowName("Asset-Action");
        when(flowRepository.findByFlowNameAndApplicationId("Asset-Action", app.getId())).thenReturn(Optional.of(childFlow));

        FlowStep activeStep = new FlowStep();
        activeStep.setId(UUID.randomUUID());
        activeStep.setStepName("ACTIVE");
        when(flowStepRepository.findByStepNameAndFlowId("ACTIVE", parentFlow.getId())).thenReturn(Optional.of(activeStep));

        FlowStep assignStep = new FlowStep();
        assignStep.setId(UUID.randomUUID());
        assignStep.setStepName("ASSIGNMENT");
        when(flowStepRepository.findByStepNameAndFlowId("ASSIGNMENT", childFlow.getId())).thenReturn(Optional.of(assignStep));

        when(flowTrackingRepository.findByExternalIdAndParentIdIsNull(externalId)).thenReturn(Optional.empty());

        UUID parentTrackingId = UUID.randomUUID();
        FlowTracking savedParent = FlowTracking.builder()
                .id(parentTrackingId)
                .flowId(parentFlow.getId())
                .externalId(externalId)
                .status("ACTIVE")
                .type("TICKET_BOOK")
                .build();

        UUID childTrackingId = UUID.randomUUID();
        FlowTracking savedChild = FlowTracking.builder()
                .id(childTrackingId)
                .parentId(parentTrackingId)
                .flowId(childFlow.getId())
                .externalId(externalId)
                .status("Deployed")
                .type("TICKET_BOOK")
                .build();

        when(flowTrackingRepository.save(any(FlowTracking.class)))
                .thenReturn(savedParent)
                .thenReturn(savedChild);

        // Act
        TrackingStatusResponse response = trackingService.processTrackingStatus(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(childTrackingId);
        assertThat(response.getParentId()).isEqualTo(parentTrackingId);
        assertThat(response.getFlowName()).isEqualTo("Asset-Action");

        // Verify parent saved and child saved
        verify(flowTrackingRepository, times(2)).save(any(FlowTracking.class));
        // Verify parent step (ACTIVE) saved + child step (ASSIGNMENT) saved
        verify(flowStepTrackingRepository, times(1)).save(any(FlowStepTracking.class));
        verify(flowStepTrackingRepository, times(1)).saveAll(anyList());
    }

    @Test
    void processTrackingStatus_childAssetAction_withEditStep_shouldResolveStepAndReturnResponse() {
        // Arrange
        UUID externalId = UUID.randomUUID();
        TrackingStatusRequest request = new TrackingStatusRequest();
        request.setApplicationName("Kahua Asset");
        request.setFlowName("Asset-Action");
        request.setExternalId(externalId);
        request.setStatus("Updated");
        request.setType("TICKET_BOOK");

        StepTrackingEntry stepEntry = new StepTrackingEntry();
        stepEntry.setStepName("EDIT");
        stepEntry.setStatus("Modified");
        stepEntry.setCount(100);
        request.setSteps(List.of(stepEntry));

        Application app = new Application();
        app.setId(UUID.randomUUID());
        app.setName("Kahua Asset");
        when(applicationRepository.findByName("Kahua Asset")).thenReturn(Optional.of(app));

        Flow parentFlow = new Flow();
        parentFlow.setId(UUID.randomUUID());
        parentFlow.setFlowName("Asset-Lifecycle");
        when(flowRepository.findByFlowNameAndApplicationId("Asset-Lifecycle", app.getId())).thenReturn(Optional.of(parentFlow));

        Flow childFlow = new Flow();
        childFlow.setId(UUID.randomUUID());
        childFlow.setFlowName("Asset-Action");
        when(flowRepository.findByFlowNameAndApplicationId("Asset-Action", app.getId())).thenReturn(Optional.of(childFlow));

        FlowStep activeStep = new FlowStep();
        activeStep.setId(UUID.randomUUID());
        activeStep.setStepName("ACTIVE");
        when(flowStepRepository.findByStepNameAndFlowId("ACTIVE", parentFlow.getId())).thenReturn(Optional.of(activeStep));

        FlowStep editStep = new FlowStep();
        editStep.setId(UUID.randomUUID());
        editStep.setStepName("EDIT");
        when(flowStepRepository.findByStepNameAndFlowId("EDIT", childFlow.getId())).thenReturn(Optional.of(editStep));

        UUID parentTrackingId = UUID.randomUUID();
        FlowTracking existingParent = FlowTracking.builder()
                .id(parentTrackingId)
                .flowId(parentFlow.getId())
                .externalId(externalId)
                .status("ACTIVE")
                .type("TICKET_BOOK")
                .build();
        when(flowTrackingRepository.findByExternalIdAndParentIdIsNull(externalId)).thenReturn(Optional.of(existingParent));

        UUID childTrackingId = UUID.randomUUID();
        FlowTracking savedChild = FlowTracking.builder()
                .id(childTrackingId)
                .parentId(parentTrackingId)
                .flowId(childFlow.getId())
                .externalId(externalId)
                .status("Updated")
                .type("TICKET_BOOK")
                .build();

        when(flowTrackingRepository.save(any(FlowTracking.class))).thenReturn(savedChild);

        // Act
        TrackingStatusResponse response = trackingService.processTrackingStatus(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(childTrackingId);
        assertThat(response.getParentId()).isEqualTo(parentTrackingId);
        assertThat(response.getFlowName()).isEqualTo("Asset-Action");
        assertThat(response.getStatus()).isEqualTo("Updated");

        verify(flowStepRepository).findByStepNameAndFlowId("EDIT", childFlow.getId());
        verify(flowStepTrackingRepository, times(1)).saveAll(anyList());
    }

    @Test
    void processTrackingStatus_childAssetAction_shouldUpdateParentStatusWhenProvided() {
        // Arrange
        UUID externalId = UUID.randomUUID();
        TrackingStatusRequest request = new TrackingStatusRequest();
        request.setApplicationName("Kahua Asset");
        request.setFlowName("Asset-Action");
        request.setExternalId(externalId);
        request.setStatus("Reconciled");
        request.setParentStatus("RECONCILED");

        StepTrackingEntry stepEntry = new StepTrackingEntry();
        stepEntry.setStepName("RETURN");
        stepEntry.setStatus("Returned");
        request.setSteps(List.of(stepEntry));

        Application app = new Application();
        app.setId(UUID.randomUUID());
        app.setName("Kahua Asset");
        when(applicationRepository.findByName("Kahua Asset")).thenReturn(Optional.of(app));

        Flow parentFlow = new Flow();
        parentFlow.setId(UUID.randomUUID());
        parentFlow.setFlowName("Asset-Lifecycle");
        when(flowRepository.findByFlowNameAndApplicationId("Asset-Lifecycle", app.getId())).thenReturn(Optional.of(parentFlow));

        Flow childFlow = new Flow();
        childFlow.setId(UUID.randomUUID());
        childFlow.setFlowName("Asset-Action");
        when(flowRepository.findByFlowNameAndApplicationId("Asset-Action", app.getId())).thenReturn(Optional.of(childFlow));

        FlowStep returnStep = new FlowStep();
        returnStep.setId(UUID.randomUUID());
        returnStep.setStepName("RETURN");
        when(flowStepRepository.findByStepNameAndFlowId("RETURN", childFlow.getId())).thenReturn(Optional.of(returnStep));

        FlowStep reconciledStep = new FlowStep();
        reconciledStep.setId(UUID.randomUUID());
        reconciledStep.setStepName("RECONCILED");
        when(flowStepRepository.findByStepNameAndFlowId("RECONCILED", parentFlow.getId())).thenReturn(Optional.of(reconciledStep));

        UUID parentTrackingId = UUID.randomUUID();
        FlowTracking existingParent = FlowTracking.builder()
                .id(parentTrackingId)
                .flowId(parentFlow.getId())
                .externalId(externalId)
                .status("ACTIVE")
                .build();
        when(flowTrackingRepository.findByExternalIdAndParentIdIsNull(externalId)).thenReturn(Optional.of(existingParent));

        UUID childTrackingId = UUID.randomUUID();
        FlowTracking savedChild = FlowTracking.builder()
                .id(childTrackingId)
                .parentId(parentTrackingId)
                .flowId(childFlow.getId())
                .externalId(externalId)
                .status("Reconciled")
                .build();

        when(flowTrackingRepository.save(any(FlowTracking.class)))
                .thenReturn(existingParent)
                .thenReturn(savedChild);

        // Act
        TrackingStatusResponse response = trackingService.processTrackingStatus(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(existingParent.getStatus()).isEqualTo("RECONCILED");
        verify(flowStepTrackingRepository, times(1)).save(any(FlowStepTracking.class)); // Parent status step
    }

    @Test
    void searchTrackingStatus_byExternalId_shouldReturnParentWithActionsHierarchy() {
        // Arrange
        UUID externalId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID child1Id = UUID.randomUUID();
        UUID child2Id = UUID.randomUUID();

        FlowTracking parent = FlowTracking.builder()
                .id(parentId)
                .externalId(externalId)
                .status("ACTIVE")
                .type("TICKET_BOOK")
                .build();

        FlowTracking child1 = FlowTracking.builder()
                .id(child1Id)
                .parentId(parentId)
                .externalId(externalId)
                .status("Created")
                .type("TICKET_BOOK")
                .build();

        FlowTracking child2 = FlowTracking.builder()
                .id(child2Id)
                .parentId(parentId)
                .externalId(externalId)
                .status("Deployed")
                .type("TICKET_BOOK")
                .build();

        when(flowTrackingRepository.findByExternalIdAndParentIdIsNull(externalId)).thenReturn(Optional.of(parent));
        when(flowTrackingRepository.findByParentIdOrderByCreatedAtAsc(parentId)).thenReturn(List.of(child1, child2));
        when(flowStepTrackingRepository.findAll(any(Specification.class))).thenReturn(List.of());

        // Act
        List<TrackingStatusResponse> responses = trackingService.searchTrackingStatus(null, externalId, null, null, null, null);

        // Assert
        assertThat(responses).hasSize(1);
        TrackingStatusResponse parentResp = responses.get(0);
        assertThat(parentResp.getId()).isEqualTo(parentId);
        assertThat(parentResp.getFlowName()).isEqualTo("Asset-Lifecycle");
        assertThat(parentResp.getActions()).hasSize(2);
        assertThat(parentResp.getActions().get(0).getId()).isEqualTo(child1Id);
        assertThat(parentResp.getActions().get(1).getId()).isEqualTo(child2Id);
    }

    @Test
    void processTrackingStatus_shouldFailFastIfApplicationNotFound() {
        // Arrange
        TrackingStatusRequest request = new TrackingStatusRequest();
        request.setApplicationName("UnknownApp");

        when(applicationRepository.findByName("UnknownApp")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> trackingService.processTrackingStatus(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Application not found");

        verifyNoInteractions(flowRepository);
    }

    @Test
    void caching_shouldCacheApplicationFlowAndFlowStepMetadata() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                CacheConfig.APPLICATION_CACHE, CacheConfig.FLOW_CACHE, CacheConfig.FLOW_STEP_CACHE);
        TrackingService cachedService = new TrackingService(
                applicationRepository, flowRepository, flowStepRepository, flowTrackingRepository, flowStepTrackingRepository, cacheManager);

        Application app = new Application();
        app.setId(UUID.randomUUID());
        app.setName("CachedApp");
        when(applicationRepository.findByName("CachedApp")).thenReturn(Optional.of(app));

        Flow flow = new Flow();
        flow.setId(UUID.randomUUID());
        flow.setFlowName("CachedFlow");
        when(flowRepository.findByFlowNameAndApplicationId("CachedFlow", app.getId())).thenReturn(Optional.of(flow));

        FlowStep step = new FlowStep();
        step.setId(UUID.randomUUID());
        step.setStepName("CachedStep");
        when(flowStepRepository.findByStepNameAndFlowId("CachedStep", flow.getId())).thenReturn(Optional.of(step));

        // First call populates cache
        Application app1 = cachedService.getApplicationByName("CachedApp");
        Flow flow1 = cachedService.getFlowByFlowNameAndApplicationId("CachedFlow", app.getId(), app.getName());
        Optional<FlowStep> step1 = cachedService.getFlowStepByStepNameAndFlowId("CachedStep", flow.getId());

        // Second call retrieves from Caffeine cache
        Application app2 = cachedService.getApplicationByName("CachedApp");
        Flow flow2 = cachedService.getFlowByFlowNameAndApplicationId("CachedFlow", app.getId(), app.getName());
        Optional<FlowStep> step2 = cachedService.getFlowStepByStepNameAndFlowId("CachedStep", flow.getId());

        assertThat(app1).isSameAs(app2);
        assertThat(flow1).isSameAs(flow2);
        assertThat(step1.get()).isSameAs(step2.get());

        // Verify underlying repositories were queried only once
        verify(applicationRepository, times(1)).findByName("CachedApp");
        verify(flowRepository, times(1)).findByFlowNameAndApplicationId("CachedFlow", app.getId());
        verify(flowStepRepository, times(1)).findByStepNameAndFlowId("CachedStep", flow.getId());
    }
}
