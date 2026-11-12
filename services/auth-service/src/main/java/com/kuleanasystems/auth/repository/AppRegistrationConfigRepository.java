package com.kuleanasystems.auth.repository;

import com.kuleanasystems.auth.entity.AppRegistrationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppRegistrationConfigRepository extends JpaRepository<AppRegistrationConfig, UUID> {
    Optional<AppRegistrationConfig> findByAppName(String appName);
}
