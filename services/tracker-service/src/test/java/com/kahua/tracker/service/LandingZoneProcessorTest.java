package com.kahua.tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kahua.tracker.dto.tracking.TrackingStatusRequest;
import com.kahua.tracker.entity.TrackingLandingZone;
import com.kahua.tracker.repository.TrackingLandingZoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LandingZoneProcessorTest {

    @Mock
    private TrackingLandingZoneRepository landingZoneRepository;

    @Mock
    private TrackingService trackingService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LandingZoneProcessor landingZoneProcessor;

    @Test
    void processLandingZoneRecord_shouldProcessRecordAndMarkAsCompleted() throws Exception {
        UUID recordId = UUID.randomUUID();
        TrackingLandingZone record = new TrackingLandingZone();
        record.setId(recordId);
        com.fasterxml.jackson.databind.JsonNode mockPayload = mock(com.fasterxml.jackson.databind.JsonNode.class);
        record.setPayload(mockPayload);
        record.setOriginalUser("testuser");
        
        when(landingZoneRepository.findById(recordId)).thenReturn(Optional.of(record));
        
        TrackingStatusRequest mockRequest = new TrackingStatusRequest();
        when(objectMapper.treeToValue(record.getPayload(), TrackingStatusRequest.class)).thenReturn(mockRequest);

        landingZoneProcessor.processLandingZoneRecord(recordId, 0);

        verify(trackingService, times(1)).processTrackingStatusAsync(eq(mockRequest), eq("testuser"));
        assertEquals("COMPLETED", record.getStatus());
        assertEquals(0, record.getRetryCount());
        assertNull(record.getErrorMessage());
    }

    @Test
    void processLandingZoneRecord_shouldRecordErrorAndRethrow_whenAttemptIsLessThanMax() throws Exception {
        UUID recordId = UUID.randomUUID();
        TrackingLandingZone record = new TrackingLandingZone();
        record.setId(recordId);
        com.fasterxml.jackson.databind.JsonNode mockPayload = mock(com.fasterxml.jackson.databind.JsonNode.class);
        record.setPayload(mockPayload);
        record.setOriginalUser("testuser");

        when(landingZoneRepository.findById(recordId)).thenReturn(Optional.of(record));

        TrackingStatusRequest mockRequest = new TrackingStatusRequest();
        when(objectMapper.treeToValue(record.getPayload(), TrackingStatusRequest.class)).thenReturn(mockRequest);

        doThrow(new RuntimeException("Transient DB timeout")).when(trackingService).processTrackingStatusAsync(eq(mockRequest), eq("testuser"));

        // On attempt 1 (retryCount = 0), should record status PROCESSING, error message, retryCount, and rethrow
        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                landingZoneProcessor.processLandingZoneRecord(recordId, 0)
        );

        assertTrue(thrown.getMessage().contains("Transient failure"));
        assertEquals("PROCESSING", record.getStatus());
        assertEquals(0, record.getRetryCount());
        assertEquals("Transient DB timeout", record.getErrorMessage());
        verify(landingZoneRepository, atLeastOnce()).saveAndFlush(record);
    }

    @Test
    void processLandingZoneRecord_shouldMarkAsFailedIfMaxRetriesExceeded() throws Exception {
        UUID recordId = UUID.randomUUID();
        TrackingLandingZone record = new TrackingLandingZone();
        record.setId(recordId);
        com.fasterxml.jackson.databind.JsonNode mockPayload = mock(com.fasterxml.jackson.databind.JsonNode.class);
        record.setPayload(mockPayload);
        record.setOriginalUser("testuser");
        
        when(landingZoneRepository.findById(recordId)).thenReturn(Optional.of(record));
        
        TrackingStatusRequest mockRequest = new TrackingStatusRequest();
        when(objectMapper.treeToValue(record.getPayload(), TrackingStatusRequest.class)).thenReturn(mockRequest);
        
        doThrow(new RuntimeException("Fatal error")).when(trackingService).processTrackingStatusAsync(eq(mockRequest), eq("testuser"));

        // On 5th attempt (retryCount = 4), should mark FAILED, record error and retryCount, and NOT rethrow exception
        assertDoesNotThrow(() -> landingZoneProcessor.processLandingZoneRecord(recordId, 4));

        assertEquals("FAILED", record.getStatus());
        assertEquals(4, record.getRetryCount());
        assertEquals("Fatal error", record.getErrorMessage());
        verify(landingZoneRepository, atLeastOnce()).saveAndFlush(record);
    }

    @Test
    void processLandingZoneRecord_shouldSkip_whenAlreadyCompleted() {
        UUID recordId = UUID.randomUUID();
        TrackingLandingZone record = new TrackingLandingZone();
        record.setId(recordId);
        record.setStatus("COMPLETED");

        when(landingZoneRepository.findById(recordId)).thenReturn(Optional.of(record));

        landingZoneProcessor.processLandingZoneRecord(recordId, 0);

        verifyNoInteractions(trackingService);
    }
}
