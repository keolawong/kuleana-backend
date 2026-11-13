package com.kahua.registration.repository;

import com.kahua.registration.entity.RegistrationSubmission;
import com.kahua.registration.entity.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RegistrationSubmissionRepository extends JpaRepository<RegistrationSubmission, UUID> {

    List<RegistrationSubmission> findByAccountUserIdOrderByCreatedAtUtcDesc(String accountUserId);

    List<RegistrationSubmission> findByOrgIdAndFormIdOrderByCreatedAtUtcDesc(UUID orgId, UUID formId);

    List<RegistrationSubmission> findByOrgIdOrderByCreatedAtUtcDesc(UUID orgId);

    long countByFormIdAndAccountUserIdAndStatusIn(UUID formId, String accountUserId, Collection<SubmissionStatus> statuses);

    long countByFormIdAndParticipantKeyAndStatusIn(UUID formId, String participantKey, Collection<SubmissionStatus> statuses);
}
