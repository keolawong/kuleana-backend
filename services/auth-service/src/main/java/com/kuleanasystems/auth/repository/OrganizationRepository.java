package com.kuleanasystems.auth.repository;

import com.kuleanasystems.auth.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    List<Organization> findAllByStatus(String status);

    Optional<Organization> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Organization> findFirstByIsDefaultTrue();
}
