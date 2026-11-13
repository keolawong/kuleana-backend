package com.kahua.registration.repository;

import com.kahua.registration.entity.RegistrationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegistrationAuditLogRepository extends JpaRepository<RegistrationAuditLog, UUID> {
    List<RegistrationAuditLog> findBySubmissionIdOrderByCreatedAtUtcDesc(UUID submissionId);
}
