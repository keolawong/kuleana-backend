package com.kahua.tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kahua.tracker.dto.tracking.TrackingStatusRequest;
import com.kahua.tracker.dto.tracking.TrackingStatusResponse;
import com.kahua.tracker.service.TrackingService;
import com.kahua.tracker.entity.TrackingLandingZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TrackingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TrackingService trackingService;

    @Mock
    private com.kahua.tracker.repository.TrackingLandingZoneRepository landingZoneRepository;

    @Mock
    private com.kahua.tracker.service.CloudTasksService cloudTasksService;

    @Mock
    private ObjectMapper objectMapperMock;

    @InjectMocks
    private TrackingController trackingController;

    private ObjectMapper objectMapper;

    private TrackingStatusResponse mockResponse;
    private UUID mockId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(trackingController).build();
        objectMapper = new ObjectMapper();

        mockId = UUID.randomUUID();
        mockResponse = new TrackingStatusResponse();
        mockResponse.setId(mockId);
        mockResponse.setStatus("IN_PROGRESS");
    }

    @Test
    void submitTrackingStatus_shouldReturn202() throws Exception {
        TrackingStatusRequest request = new TrackingStatusRequest();
        request.setStatus("IN_PROGRESS");

        // Mock SecurityContext
        org.springframework.security.core.Authentication authentication = org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.context.SecurityContext securityContext = org.mockito.Mockito.mock(org.springframework.security.core.context.SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);

        com.fasterxml.jackson.databind.JsonNode mockNode = org.mockito.Mockito.mock(com.fasterxml.jackson.databind.JsonNode.class);
        when(objectMapperMock.valueToTree(any())).thenReturn(mockNode);

        TrackingLandingZone mockSavedLz = new TrackingLandingZone();
        mockSavedLz.setId(UUID.randomUUID());
        when(landingZoneRepository.save(any())).thenReturn(mockSavedLz);

        mockMvc.perform(post("/api/tracking/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    @Test
    void submitBulkTrackingStatus_shouldReturn202() throws Exception {
        TrackingStatusRequest req1 = new TrackingStatusRequest();
        req1.setStatus("IN_PROGRESS");
        TrackingStatusRequest req2 = new TrackingStatusRequest();
        req2.setStatus("COMPLETED");

        org.springframework.security.core.Authentication authentication = org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.context.SecurityContext securityContext = org.mockito.Mockito.mock(org.springframework.security.core.context.SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);

        com.fasterxml.jackson.databind.JsonNode mockNode = org.mockito.Mockito.mock(com.fasterxml.jackson.databind.JsonNode.class);
        when(objectMapperMock.valueToTree(any())).thenReturn(mockNode);

        TrackingLandingZone lz1 = new TrackingLandingZone();
        lz1.setId(UUID.randomUUID());
        TrackingLandingZone lz2 = new TrackingLandingZone();
        lz2.setId(UUID.randomUUID());
        when(landingZoneRepository.saveAll(any())).thenReturn(List.of(lz1, lz2));

        mockMvc.perform(post("/api/tracking/status/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(req1, req2))))
                .andExpect(status().isAccepted());
    }

    @Test
    void submitBulkTrackingStatus_shouldReturn400IfEmpty() throws Exception {
        mockMvc.perform(post("/api/tracking/status/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchTrackingStatus_shouldReturn200() throws Exception {
        when(trackingService.searchTrackingStatus(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/tracking/status")
                .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"));
    }

    @Test
    void searchTrackingStatus_shouldReturn204IfEmpty() throws Exception {
        when(trackingService.searchTrackingStatus(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/tracking/status"))
                .andExpect(status().isNoContent());
    }
}
