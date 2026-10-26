package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.GateCaptain;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GateCaptainSpecification {

    public static Specification<GateCaptain> search(
            UUID gateCaptainId, String captainName, UUID gateId, UUID operationId, UUID shiftId,
            String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (gateCaptainId != null) {
                predicates.add(cb.equal(root.get("gateCaptainId"), gateCaptainId));
            }
            if (captainName != null && !captainName.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("captainName")), "%" + captainName.toLowerCase() + "%"));
            }
            if (gateId != null) {
                predicates.add(cb.equal(root.join("gate").get("gateId"), gateId));
            }
            if (operationId != null) {
                predicates.add(cb.equal(root.join("operation").get("operationId"), operationId));
            }
            if (shiftId != null) {
                predicates.add(cb.equal(root.join("shift").get("shiftId"), shiftId));
            }
            if (createdBy != null && !createdBy.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("createdBy"), createdBy));
            }
            if (createdAt != null) {
                predicates.add(cb.equal(root.get("createdAt"), createdAt));
            }
            if (updatedBy != null && !updatedBy.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("updatedBy"), updatedBy));
            }
            if (updatedAt != null) {
                predicates.add(cb.equal(root.get("updatedAt"), updatedAt));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}