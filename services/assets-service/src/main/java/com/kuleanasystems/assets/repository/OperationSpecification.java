package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.Operation;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OperationSpecification {

    public static Specification<Operation> search(
            UUID operationId, String name, LocalDate startDate, LocalDate endDate,
            String locationName, Boolean isActive, BigDecimal ticketPrice, BigDecimal ccFee,
            String ccFeeType, String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (operationId != null) {
                predicates.add(cb.equal(root.get("operationId"), operationId));
            }
            if (name != null && !name.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (startDate != null) {
                predicates.add(cb.equal(root.get("startDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.equal(root.get("endDate"), endDate));
            }
            if (locationName != null && !locationName.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("locationName")), "%" + locationName.toLowerCase() + "%"));
            }
            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }
            if (ticketPrice != null) {
                predicates.add(cb.equal(root.get("ticketPrice"), ticketPrice));
            }
            if (ccFee != null) {
                predicates.add(cb.equal(root.get("ccFee"), ccFee));
            }
            if (ccFeeType != null && !ccFeeType.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("ccFeeType"), ccFeeType));
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