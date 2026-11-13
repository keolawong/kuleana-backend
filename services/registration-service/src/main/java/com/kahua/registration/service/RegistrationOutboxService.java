package com.kahua.registration.service;

import com.kahua.registration.entity.OutboxStatus;
import com.kahua.registration.entity.RegistrationOutbox;
import com.kahua.registration.entity.SyncStatus;
import com.kahua.registration.repository.RegistrationOutboxRepository;
import com.kahua.registration.repository.RegistrationSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationOutboxService {

    private static final int MAX_ATTEMPTS = 5;

    private final RegistrationOutboxRepository outboxRepository;
    private final RegistrationSubmissionRepository submissionRepository;
    private final AssetsPersonSyncAdapter assetsPersonSyncAdapter;
    private final Clock clock;

    public boolean processOutboxRecord(UUID outboxId, int retryCountHeader) {
        RegistrationOutbox outbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalArgumentException("Outbox record not found: " + outboxId));

        if (outbox.getStatus() == OutboxStatus.SYNCED) {
            return true;
        }

        try {
            assetsPersonSyncAdapter.syncSubmission(outbox);
            markSynced(outboxId);
            return true;
        } catch (Exception ex) {
            int attempt = Math.max(outbox.getRetryCount() + 1, retryCountHeader + 1);
            if (attempt >= MAX_ATTEMPTS) {
                log.error("Outbox {} reached max attempts ({}); marking FAILED: {}", outboxId, attempt, ex.getMessage());
                markFailed(outboxId, attempt);
                return false;
            }
            log.warn("Outbox {} failed attempt {}/{}; scheduling retry: {}", outboxId, attempt, MAX_ATTEMPTS, ex.getMessage());
            recordRetry(outboxId, attempt);
            throw new IllegalStateException("Outbox processing failed on attempt " + attempt, ex);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markSynced(UUID outboxId) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        RegistrationOutbox outbox = outboxRepository.findById(outboxId).orElseThrow();
        outbox.setStatus(OutboxStatus.SYNCED);
        outbox.setProcessedAtUtc(now);
        outboxRepository.save(outbox);

        submissionRepository.findById(outbox.getSubmissionId()).ifPresent(sub -> {
            sub.setSyncStatus(SyncStatus.SYNCED);
            submissionRepository.save(sub);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void recordRetry(UUID outboxId, int attempt) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        RegistrationOutbox outbox = outboxRepository.findById(outboxId).orElseThrow();
        outbox.setRetryCount(attempt);
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setNextAttemptAtUtc(now.plusSeconds((long) Math.pow(2, attempt) * 10L));
        outboxRepository.save(outbox);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markFailed(UUID outboxId, int attempt) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        RegistrationOutbox outbox = outboxRepository.findById(outboxId).orElseThrow();
        outbox.setRetryCount(attempt);
        outbox.setStatus(OutboxStatus.FAILED);
        outbox.setProcessedAtUtc(now);
        outboxRepository.save(outbox);

        submissionRepository.findById(outbox.getSubmissionId()).ifPresent(sub -> {
            sub.setSyncStatus(SyncStatus.FAILED);
            submissionRepository.save(sub);
        });
    }
}
