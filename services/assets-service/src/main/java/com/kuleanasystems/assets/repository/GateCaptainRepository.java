package com.kuleanasystems.assets.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import org.springframework.lang.Nullable;

import com.kuleanasystems.assets.entity.GateCaptain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GateCaptainRepository extends JpaRepository<GateCaptain, UUID>, JpaSpecificationExecutor<GateCaptain> {

    @Override
    @EntityGraph(attributePaths = {"operation", "gate", "shift"})
    List<GateCaptain> findAll(@Nullable Specification<GateCaptain> spec);

}