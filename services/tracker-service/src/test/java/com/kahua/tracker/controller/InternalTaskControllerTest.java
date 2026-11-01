package com.kahua.tracker.controller;

import com.kahua.tracker.entity.TrackingLandingZone;
import com.kahua.tracker.repository.TrackingLandingZoneRepository;
import com.kahua.tracker.service.CloudTasksService;
import com.kahua.tracker.service.LandingZoneProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalTaskControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LandingZoneProcessor landingZoneProcessor;

    @Mock
    private TrackingLandingZoneRepository landingZoneRepository;

    @Mock
    private CloudTasksService cloudTasksService;

    @InjectMocks
    private InternalTaskController internalTaskController;

    private static final String TEST_API_KEY = "test-secret-key";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(internalTaskController).build();
        ReflectionTestUtils.setField(internalTaskController, "internalApiKey", TEST_API_KEY);
    }

    @Test
    void processLandingZone_shouldReturn401_whenApiKeyIsMissingOrInvalid() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/internal/tasks/process-landing-zone/{id}", id))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/internal/tasks/process-landing-zone/{id}", id)
                        .header("X-Internal-Task-Key", "invalid-key"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(landingZoneProcessor);
    }

    @Test
    void processLandingZone_shouldPassRetryCount_andReturn200() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/internal/tasks/process-landing-zone/{id}", id)
                        .header("X-Internal-Task-Key", TEST_API_KEY)
                        .header("X-CloudTasks-TaskRetryCount", "3"))
                .andExpect(status().isOk());

        verify(landingZoneProcessor).processLandingZoneRecord(eq(id), eq(3));
    }

    @Test
    void processLandingZone_shouldReturn500_whenProcessorThrowsException() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("Processing failed")).when(landingZoneProcessor).processLandingZoneRecord(eq(id), eq(0));

        mockMvc.perform(post("/api/internal/tasks/process-landing-zone/{id}", id)
                        .header("X-Internal-Task-Key", TEST_API_KEY))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void sweepLandingZone_shouldFindStaleRecordsAndEnqueue() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        TrackingLandingZone r1 = TrackingLandingZone.builder().id(id1).build();
        TrackingLandingZone r2 = TrackingLandingZone.builder().id(id2).build();

        when(landingZoneRepository.findStaleLandingZoneRecords(any(), any())).thenReturn(List.of(r1, r2));

        mockMvc.perform(post("/api/internal/tasks/sweep-landing-zone")
                        .header("X-Internal-Task-Key", TEST_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));

        verify(cloudTasksService).enqueueLandingZoneTask(id1);
        verify(cloudTasksService).enqueueLandingZoneTask(id2);
    }
}
