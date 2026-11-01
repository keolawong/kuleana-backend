package com.kahua.tracker.spec;

import com.kahua.tracker.entity.FlowStep;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FlowStepSpecification {

    public static Specification<FlowStep> findByCriteria(
            UUID flowId,
            String stepName,
            Integer stepSeqNum) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (flowId != null) {
                predicates.add(criteriaBuilder.equal(root.get("flowId"), flowId));
            }
            if (stepName != null && !stepName.isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("stepName")), "%" + stepName.toLowerCase() + "%"));
            }
            if (stepSeqNum != null) {
                predicates.add(criteriaBuilder.equal(root.get("stepSeqNum"), stepSeqNum));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
