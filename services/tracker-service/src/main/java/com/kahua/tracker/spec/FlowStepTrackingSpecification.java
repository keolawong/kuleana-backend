package com.kahua.tracker.spec;

import com.kahua.tracker.entity.FlowStepTracking;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FlowStepTrackingSpecification {

    public static Specification<FlowStepTracking> findByCriteria(
            UUID flowTrackingId,
            UUID flowStepId,
            String status,
            BigDecimal amount,
            Integer count) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (flowTrackingId != null) {
                predicates.add(criteriaBuilder.equal(root.get("flowTrackingId"), flowTrackingId));
            }
            if (flowStepId != null) {
                predicates.add(criteriaBuilder.equal(root.get("flowStepId"), flowStepId));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("status")), "%" + status.toLowerCase() + "%"));
            }
            if (amount != null) {
                predicates.add(criteriaBuilder.equal(root.get("amount"), amount));
            }
            if (count != null) {
                predicates.add(criteriaBuilder.equal(root.get("count"), count));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
