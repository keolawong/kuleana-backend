package com.kahua.registration.repository;

import com.kahua.registration.entity.RegistrationForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationFormRepository extends JpaRepository<RegistrationForm, UUID> {
    Optional<RegistrationForm> findBySlugAndIsActiveTrue(String slug);
    Optional<RegistrationForm> findBySlug(String slug);
    Optional<RegistrationForm> findByOrgIdAndSlug(UUID orgId, String slug);
    List<RegistrationForm> findByOrgIdOrderByCreatedAtUtcDesc(UUID orgId);
}
