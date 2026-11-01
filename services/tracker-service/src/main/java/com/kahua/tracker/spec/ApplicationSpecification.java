package com.kahua.tracker.spec;

import com.kahua.tracker.entity.Application;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ApplicationSpecification {

    public static Specification<Application> findByCriteria(
            String name,
            UUID writeRoleId,
            UUID readRoleId,
            Boolean isActive) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (writeRoleId != null) {
                predicates.add(criteriaBuilder.equal(root.get("writeRoleId"), writeRoleId));
            }
            if (readRoleId != null) {
                predicates.add(criteriaBuilder.equal(root.get("readRoleId"), readRoleId));
            }
            if (isActive != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), isActive));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
