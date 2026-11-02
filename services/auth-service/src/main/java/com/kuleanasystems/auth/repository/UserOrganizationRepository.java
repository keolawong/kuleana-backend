package com.kuleanasystems.auth.repository;

import com.kuleanasystems.auth.entity.UserOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, UUID> {

    List<UserOrganization> findByUserId(UUID userId);

    List<UserOrganization> findByUserIdIn(List<UUID> userIds);

    List<UserOrganization> findByOrgId(UUID orgId);

    Optional<UserOrganization> findByUserIdAndOrgId(UUID userId, UUID orgId);

    @Modifying
    @Query("DELETE FROM UserOrganization uo WHERE uo.userId = :userId AND uo.orgId = :orgId")
    void deleteByUserIdAndOrgId(@Param("userId") UUID userId, @Param("orgId") UUID orgId);
}
