package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.CreditReport;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreditReportSpecification {

    public static Specification<CreditReport> search(
            UUID creditReportId, BigDecimal totalAmount, UUID operationId, UUID shiftId,
            UUID gateId, String deviceId, UUID processId, UUID personStickerId, String createdBy,
            Instant createdAt, String updatedBy, Instant updatedAt) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (creditReportId != null) {
                predicates.add(cb.equal(root.get("creditReportId"), creditReportId));
            }
            if (totalAmount != null) {
                predicates.add(cb.equal(root.get("totalAmount"), totalAmount));
            }
            if (operationId != null) {
                predicates.add(cb.equal(root.join("operation").get("operationId"), operationId));
            }
            if (shiftId != null) {
                predicates.add(cb.equal(root.join("shift").get("shiftId"), shiftId));
            }
            if (gateId != null) {
                predicates.add(cb.equal(root.join("gate").get("gateId"), gateId));
            }
            if (deviceId != null && !deviceId.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("deviceId"), deviceId));
            }
            if (processId != null) {
                predicates.add(cb.equal(root.get("processId"), processId));
            }
            if (personStickerId != null) {
                predicates.add(cb.equal(root.join("person").get("personId"), personStickerId));
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