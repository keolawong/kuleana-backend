package com.kahua.registration.repository;

import com.kahua.registration.entity.OutboxStatus;
import com.kahua.registration.entity.RegistrationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface RegistrationOutboxRepository extends JpaRepository<RegistrationOutbox, UUID> {
    List<RegistrationOutbox> findByStatusAndNextAttemptAtUtcLessThanEqual(OutboxStatus status, OffsetDateTime cutoff);
    List<RegistrationOutbox> findBySubmissionId(UUID submissionId);
}
