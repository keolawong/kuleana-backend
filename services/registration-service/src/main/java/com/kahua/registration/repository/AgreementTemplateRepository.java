package com.kahua.registration.repository;

import com.kahua.registration.entity.AgreementTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgreementTemplateRepository extends JpaRepository<AgreementTemplate, UUID> {
    List<AgreementTemplate> findByOrgIdAndIsActiveTrueOrderByTemplateKeyAscVersionDesc(UUID orgId);
    List<AgreementTemplate> findByOrgIdOrderByTemplateKeyAscVersionDesc(UUID orgId);
    Optional<AgreementTemplate> findTopByOrgIdAndTemplateKeyOrderByVersionDesc(UUID orgId, String templateKey);
}
