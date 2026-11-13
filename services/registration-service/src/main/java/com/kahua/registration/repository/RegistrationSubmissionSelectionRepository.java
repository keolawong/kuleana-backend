package com.kahua.registration.repository;

import com.kahua.registration.entity.RegistrationSubmissionSelection;
import com.kahua.registration.entity.SelectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationSubmissionSelectionRepository extends JpaRepository<RegistrationSubmissionSelection, UUID> {
    List<RegistrationSubmissionSelection> findBySubmissionId(UUID submissionId);
    List<RegistrationSubmissionSelection> findBySubmissionIdAndStatus(UUID submissionId, SelectionStatus status);
    Optional<RegistrationSubmissionSelection> findBySubmissionIdAndOptionIdAndStatus(UUID submissionId, UUID optionId, SelectionStatus status);
}
