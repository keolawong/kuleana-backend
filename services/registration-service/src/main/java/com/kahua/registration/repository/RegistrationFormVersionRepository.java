package com.kahua.registration.repository;

import com.kahua.registration.entity.RegistrationFormVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationFormVersionRepository extends JpaRepository<RegistrationFormVersion, UUID> {
    Optional<RegistrationFormVersion> findTopByFormIdOrderByVersionDesc(UUID formId);
    List<RegistrationFormVersion> findByFormIdOrderByVersionDesc(UUID formId);
    Optional<RegistrationFormVersion> findByFormIdAndVersion(UUID formId, Integer version);
}
