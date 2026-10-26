package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.Operation;
import com.kuleanasystems.assets.entity.Shift;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShiftSpecification {

    public static Specification<Shift> search(
            UUID shiftId, UUID operationId, String name, Instant startTime, Instant endTime,
            String traffic, String status, String createdBy, Instant createdAt,
            String updatedBy, Instant updatedAt, Boolean activeOperation) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (shiftId != null) {
                predicates.add(cb.equal(root.get("shiftId"), shiftId));
            }

            Join<Shift, Operation> operationJoin = null;
            if (operationId != null || activeOperation != null) {
                operationJoin = root.join("operation");
            }

            if (operationId != null) {
                predicates.add(cb.equal(operationJoin.get("operationId"), operationId));
            }
            if (name != null && !name.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (startTime != null) {
                predicates.add(cb.equal(root.get("startTime"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.equal(root.get("endTime"), endTime));
            }
            if (traffic != null && !traffic.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("traffic"), traffic));
            }
            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
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
            if (activeOperation != null) {
                predicates.add(cb.equal(operationJoin.get("isActive"), activeOperation));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}