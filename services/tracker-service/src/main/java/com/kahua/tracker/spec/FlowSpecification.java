package com.kahua.tracker.spec;

import com.kahua.tracker.entity.Flow;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FlowSpecification {

    public static Specification<Flow> findByCriteria(
            String flowName,
            UUID applicationId,
            Boolean isActive) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (flowName != null && !flowName.isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("flowName")), "%" + flowName.toLowerCase() + "%"));
            }
            if (applicationId != null) {
                predicates.add(criteriaBuilder.equal(root.get("applicationId"), applicationId));
            }
            if (isActive != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), isActive));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
