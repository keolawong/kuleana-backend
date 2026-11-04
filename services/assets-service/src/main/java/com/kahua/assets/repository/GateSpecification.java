package com.kahua.assets.repository;

import com.kahua.assets.entity.Gate;
import com.kahua.assets.entity.Operation;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GateSpecification {

    public static Specification<Gate> search(
            UUID gateId, UUID operationId, UUID orgId, String name, Boolean isActive, Integer cashBanksRequiredForBusy,
            Integer cashBanksRequiredForModerate, Integer cashBanksRequiredForLite,
            Integer displayOrder, String createdBy, Instant createdAt,
            String updatedBy, Instant updatedAt) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (gateId != null) {
                predicates.add(cb.equal(root.get("gateId"), gateId));
            }

            Join<Gate, Operation> operationJoin = null;
            if (operationId != null || orgId != null) {
                operationJoin = root.join("operation");
            }

            if (operationId != null) {
                predicates.add(cb.equal(operationJoin.get("operationId"), operationId));
            }
            if (orgId != null) {
                predicates.add(cb.equal(operationJoin.get("orgId"), orgId));
            }

            if (name != null && !name.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }
            if (cashBanksRequiredForBusy != null) {
                predicates.add(cb.equal(root.get("cashBanksRequiredForBusy"), cashBanksRequiredForBusy));
            }
            if (cashBanksRequiredForModerate != null) {
                predicates.add(cb.equal(root.get("cashBanksRequiredForModerate"), cashBanksRequiredForModerate));
            }
            if (cashBanksRequiredForLite != null) {
                predicates.add(cb.equal(root.get("cashBanksRequiredForLite"), cashBanksRequiredForLite));
            }
            if (displayOrder != null) {
                predicates.add(cb.equal(root.get("displayOrder"), displayOrder));
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
