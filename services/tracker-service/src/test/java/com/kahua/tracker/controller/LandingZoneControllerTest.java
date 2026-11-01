package com.kahua.tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.kahua.tracker.dto.tracking.ReprocessBulkRequest;
import com.kahua.tracker.entity.TrackingLandingZone;
import com.kahua.tracker.repository.TrackingLandingZoneRepository;
import com.kahua.tracker.service.CloudTasksService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.servlet.http.Cookie;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LandingZoneControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TrackingLandingZoneRepository landingZoneRepository;

    @Mock
    private CloudTasksService cloudTasksService;

    @InjectMocks
    private LandingZoneController landingZoneController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(landingZoneController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getLandingZoneRecords_shouldReturnMappedRecords() throws Exception {
        UUID id = UUID.randomUUID();
        TrackingLandingZone record = TrackingLandingZone.builder()
                .id(id)
                .payload(JsonNodeFactory.instance.objectNode().put("test", "val"))
                .originalUser("user1")
                .status("FAILED")
                .claimedBy("host1")
                .retryCount(4)
                .errorMessage("Fatal error")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(landingZoneRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(record));

        mockMvc.perform(get("/api/tracking/landing-zone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[0].retryCount").value(4))
                .andExpect(jsonPath("$[0].errorMessage").value("Fatal error"));
    }

    @Test
    void reprocessRecord_shouldResetStatusAndRetryCountAndEnqueue() throws Exception {
        UUID id = UUID.randomUUID();
        TrackingLandingZone record = TrackingLandingZone.builder()
                .id(id)
                .payload(JsonNodeFactory.instance.objectNode())
                .originalUser("user1")
                .status("FAILED")
                .retryCount(4)
                .errorMessage("Some error")
                .build();

        when(landingZoneRepository.findById(id)).thenReturn(Optional.of(record));

        mockMvc.perform(post("/api/tracking/landing-zone/{id}/reprocess", id))
                .andExpect(status().isAccepted());

        verify(landingZoneRepository).save(argThat(r ->
                "LANDED".equals(r.getStatus()) && r.getRetryCount() == 0 && r.getErrorMessage() == null
        ));
        verify(cloudTasksService).enqueueLandingZoneTask(id);
    }

    @Test
    void reprocessBulk_shouldReprocessByIds() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        TrackingLandingZone r1 = TrackingLandingZone.builder().id(id1).status("FAILED").retryCount(2).build();
        TrackingLandingZone r2 = TrackingLandingZone.builder().id(id2).status("FAILED").retryCount(3).build();

        when(landingZoneRepository.findAllById(List.of(id1, id2))).thenReturn(List.of(r1, r2));

        ReprocessBulkRequest request = ReprocessBulkRequest.builder().recordIds(List.of(id1, id2)).build();

        mockMvc.perform(post("/api/tracking/landing-zone/reprocess-bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.count").value(2));

        verify(landingZoneRepository, times(2)).save(argThat(r ->
                "LANDED".equals(r.getStatus()) && r.getRetryCount() == 0 && r.getErrorMessage() == null
        ));
        verify(cloudTasksService).enqueueLandingZoneTask(id1);
        verify(cloudTasksService).enqueueLandingZoneTask(id2);
    }

    @Test
    void reprocessBulk_shouldReprocessByStatus_withEmptyBody() throws Exception {
        UUID id1 = UUID.randomUUID();
        TrackingLandingZone r1 = TrackingLandingZone.builder().id(id1).status("FAILED").retryCount(4).build();

        when(landingZoneRepository.findByStatus("FAILED")).thenReturn(List.of(r1));

        mockMvc.perform(post("/api/tracking/landing-zone/reprocess-bulk")
                        .param("status", "FAILED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.count").value(1));

        verify(cloudTasksService).enqueueLandingZoneTask(id1);
    }

    @Test
    void reprocessBulk_shouldReprocessByStatusInRequestBody() throws Exception {
        UUID id1 = UUID.randomUUID();
        TrackingLandingZone r1 = TrackingLandingZone.builder().id(id1).status("LANDED").retryCount(0).build();

        when(landingZoneRepository.findByStatus("LANDED")).thenReturn(List.of(r1));

        ReprocessBulkRequest request = ReprocessBulkRequest.builder().status("LANDED").build();

        mockMvc.perform(post("/api/tracking/landing-zone/reprocess-bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.count").value(1));

        verify(cloudTasksService).enqueueLandingZoneTask(id1);
    }

    @Test
    void reprocessAllFailed_shouldDelegateToBulkReprocess() throws Exception {
        UUID id1 = UUID.randomUUID();
        TrackingLandingZone r1 = TrackingLandingZone.builder().id(id1).status("FAILED").retryCount(4).build();

        when(landingZoneRepository.findByStatus("FAILED")).thenReturn(List.of(r1));

        mockMvc.perform(post("/api/tracking/landing-zone/reprocess-failed"))
                .andExpect(status().isAccepted());

        verify(cloudTasksService).enqueueLandingZoneTask(id1);
    }

    @Test
    void streamLandingZoneRecords_successfulStream_emitsOpenChunkAndComplete() throws Exception {
        UUID id = UUID.randomUUID();
        TrackingLandingZone record = TrackingLandingZone.builder()
                .id(id)
                .payload(JsonNodeFactory.instance.objectNode().put("test", "stream"))
                .originalUser("user1")
                .status("LANDED")
                .createdAt(OffsetDateTime.now())
                .build();

        when(landingZoneRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(record)));

        org.springframework.test.web.servlet.MvcResult mvcResult = mockMvc.perform(get("/api/tracking/landing-zone/stream"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        verify(landingZoneRepository, timeout(2000)).findAll(any(Pageable.class));

        // Wait briefly for async completion
        Thread.sleep(200);

        String content = mvcResult.getResponse().getContentAsString();
        assertTrue(content.contains("OPEN") || content.contains("connected"), "Should contain OPEN event");
        assertTrue(content.contains("CHUNK") || content.contains(id.toString()), "Should contain CHUNK event");
        assertTrue(content.contains("STREAM_COMPLETE") || content.contains("totalCount"), "Should contain STREAM_COMPLETE event");
    }

    @Test
    void streamLandingZoneRecords_withStatusFilter() throws Exception {
        UUID id = UUID.randomUUID();
        TrackingLandingZone record = TrackingLandingZone.builder()
                .id(id)
                .payload(JsonNodeFactory.instance.objectNode())
                .originalUser("user1")
                .status("FAILED")
                .createdAt(OffsetDateTime.now())
                .build();

        when(landingZoneRepository.findByStatus(eq("FAILED"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        mockMvc.perform(get("/api/tracking/landing-zone/stream")
                        .param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        verify(landingZoneRepository, timeout(2000)).findByStatus(eq("FAILED"), any(Pageable.class));
    }

    @Test
    void streamLandingZoneRecords_withStatusAndDateRangeFilter() throws Exception {
        UUID id = UUID.randomUUID();
        TrackingLandingZone record = TrackingLandingZone.builder()
                .id(id)
                .payload(JsonNodeFactory.instance.objectNode())
                .originalUser("user1")
                .status("FAILED")
                .createdAt(OffsetDateTime.now())
                .build();

        when(landingZoneRepository.findByStatusAndCreatedAtBetween(eq("FAILED"), any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        mockMvc.perform(get("/api/tracking/landing-zone/stream")
                        .param("status", "FAILED")
                        .param("startDate", "2026-09-01T00:00:00Z")
                        .param("endDate", "2026-09-07T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        verify(landingZoneRepository, timeout(2000)).findByStatusAndCreatedAtBetween(
                eq("FAILED"), any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class)
        );
    }

    @Test
    void streamLandingZoneRecords_withDateRangeOnly() throws Exception {
        UUID id = UUID.randomUUID();
        TrackingLandingZone record = TrackingLandingZone.builder()
                .id(id)
                .payload(JsonNodeFactory.instance.objectNode())
                .originalUser("user1")
                .status("LANDED")
                .createdAt(OffsetDateTime.now())
                .build();

        when(landingZoneRepository.findAllByCreatedAtBetween(any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        mockMvc.perform(get("/api/tracking/landing-zone/stream")
                        .param("startDate", "2026-09-01T00:00:00Z")
                        .param("endDate", "2026-09-07T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        verify(landingZoneRepository, timeout(2000)).findAllByCreatedAtBetween(
                any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class)
        );
    }

    @Test
    void streamLandingZoneRecords_withTokenParamHeaderAndCookie() throws Exception {
        when(landingZoneRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/tracking/landing-zone/stream")
                        .param("token", "sample-query-token"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        mockMvc.perform(get("/api/tracking/landing-zone/stream")
                        .header("Authorization", "Bearer sample-bearer-token"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        mockMvc.perform(get("/api/tracking/landing-zone/stream")
                        .cookie(new Cookie("jwt", "sample-cookie-token")))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }
}
