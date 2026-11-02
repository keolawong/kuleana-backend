package com.kuleanasystems.auth.repository;

import com.kuleanasystems.auth.entity.UserApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserApplicationRepository extends JpaRepository<UserApplication, UUID> {

    List<UserApplication> findByUserId(UUID userId);

    List<UserApplication> findByUserIdIn(List<UUID> userIds);

    Optional<UserApplication> findByUserIdAndAppName(UUID userId, String appName);

    @Modifying
    @Query("DELETE FROM UserApplication ua WHERE ua.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM UserApplication ua WHERE ua.userId = :userId AND ua.appName = :appName")
    void deleteByUserIdAndAppName(@Param("userId") UUID userId, @Param("appName") String appName);
}
