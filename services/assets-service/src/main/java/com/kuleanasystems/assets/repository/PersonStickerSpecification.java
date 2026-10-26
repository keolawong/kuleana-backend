package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.PersonSticker;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PersonStickerSpecification {

    public static Specification<PersonSticker> search(
            UUID personStickerId, UUID personId, UUID shiftId, UUID gateId,
            String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (personStickerId != null) {
                predicates.add(cb.equal(root.get("personStickerId"), personStickerId));
            }
            if (personId != null) {
                predicates.add(cb.equal(root.join("person").get("personId"), personId));
            }
            if (shiftId != null) {
                predicates.add(cb.equal(root.join("shift").get("shiftId"), shiftId));
            }
            if (gateId != null) {
                predicates.add(cb.equal(root.join("gate").get("gateId"), gateId));
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
