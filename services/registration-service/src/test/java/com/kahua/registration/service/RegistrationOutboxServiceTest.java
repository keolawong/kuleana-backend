package com.kahua.registration.service;

import com.kahua.registration.entity.OutboxStatus;
import com.kahua.registration.entity.RegistrationOutbox;
import com.kahua.registration.entity.RegistrationSubmission;
import com.kahua.registration.entity.SyncStatus;
import com.kahua.registration.repository.RegistrationOutboxRepository;
import com.kahua.registration.repository.RegistrationSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationOutboxServiceTest {

    @Mock
    private RegistrationOutboxRepository outboxRepository;

    @Mock
    private RegistrationSubmissionRepository submissionRepository;

    @Mock
    private AssetsPersonSyncAdapter assetsPersonSyncAdapter;

    private RegistrationOutboxService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-01T12:00:00Z"), ZoneOffset.UTC);
        service = new RegistrationOutboxService(outboxRepository, submissionRepository, assetsPersonSyncAdapter, clock);
    }

    @Test
    void marksSyncedOnSuccess() {
        UUID outboxId = UUID.randomUUID();
        UUID subId = UUID.randomUUID();
        RegistrationOutbox outbox = RegistrationOutbox.builder()
                .id(outboxId)
                .submissionId(subId)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        RegistrationSubmission submission = RegistrationSubmission.builder()
                .id(subId)
                .syncStatus(SyncStatus.PENDING)
                .build();

        when(outboxRepository.findById(outboxId)).thenReturn(Optional.of(outbox));
        when(submissionRepository.findById(subId)).thenReturn(Optional.of(submission));

        boolean result = service.processOutboxRecord(outboxId, 0);
        assertTrue(result);
        assertEquals(OutboxStatus.SYNCED, outbox.getStatus());
        assertEquals(SyncStatus.SYNCED, submission.getSyncStatus());
    }

    @Test
    void throwsForTransientRetryAndMarksFailedOnFinalAttempt() {
        UUID outboxId = UUID.randomUUID();
        UUID subId = UUID.randomUUID();
        RegistrationOutbox outbox = RegistrationOutbox.builder()
                .id(outboxId)
                .submissionId(subId)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        RegistrationSubmission submission = RegistrationSubmission.builder()
                .id(subId)
                .syncStatus(SyncStatus.PENDING)
                .build();

        when(outboxRepository.findById(outboxId)).thenReturn(Optional.of(outbox));
        doThrow(new RuntimeException("assets-service 503")).when(assetsPersonSyncAdapter).syncSubmission(any());

        // Attempt 1 -> throws so Cloud Tasks retries
        assertThrows(IllegalStateException.class, () -> service.processOutboxRecord(outboxId, 0));
        assertEquals(1, outbox.getRetryCount());
        assertEquals(OutboxStatus.PENDING, outbox.getStatus());

        // Attempt 5 (retryCountHeader = 4) -> marks FAILED and returns false (HTTP 200 to stop retrying)
        when(submissionRepository.findById(subId)).thenReturn(Optional.of(submission));
        boolean result = service.processOutboxRecord(outboxId, 4);
        assertFalse(result);
        assertEquals(5, outbox.getRetryCount());
        assertEquals(OutboxStatus.FAILED, outbox.getStatus());
        assertEquals(SyncStatus.FAILED, submission.getSyncStatus());
    }
}
