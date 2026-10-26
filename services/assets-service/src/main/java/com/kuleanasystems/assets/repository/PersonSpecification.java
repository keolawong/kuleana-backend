package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.Person;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PersonSpecification {

    public static Specification<Person> search(
            UUID personId, String firstName, String lastName, String notes, String externalId,
            UUID shiftId, UUID gateId, String organization, String jobRole, Boolean isActive,
            String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (personId != null) {
                predicates.add(cb.equal(root.get("personId"), personId));
            }
            if (shiftId != null) {
                predicates.add(cb.equal(root.join("shift").get("shiftId"), shiftId));
            }
            if (gateId != null) {
                predicates.add(cb.equal(root.join("gate").get("gateId"), gateId));
            }
            if (organization != null && !organization.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("organization")), "%" + organization.toLowerCase() + "%"));
            }
            if (jobRole != null && !jobRole.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("jobRole")), jobRole.toLowerCase().trim()));
            }
            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }
            if (firstName != null && !firstName.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("firstName")), "%" + firstName.toLowerCase() + "%"));
            }
            if (lastName != null && !lastName.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("lastName")), "%" + lastName.toLowerCase() + "%"));
            }
            if (notes != null && !notes.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("notes")), "%" + notes.toLowerCase() + "%"));
            }
            if (externalId != null && !externalId.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("externalId"), externalId));
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
