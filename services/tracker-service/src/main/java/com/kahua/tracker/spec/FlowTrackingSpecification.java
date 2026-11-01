package com.kahua.tracker.spec;

import com.kahua.tracker.entity.FlowTracking;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FlowTrackingSpecification {

    public static Specification<FlowTracking> findByCriteria(
            UUID flowId,
            UUID externalId,
            String status,
            String type,
            UUID sessionId,
            UUID gateId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (flowId != null) {
                predicates.add(criteriaBuilder.equal(root.get("flowId"), flowId));
            }
            if (externalId != null) {
                predicates.add(criteriaBuilder.equal(root.get("externalId"), externalId));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("status")), "%" + status.toLowerCase() + "%"));
            }
            if (type != null && !type.isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("type")), "%" + type.toLowerCase() + "%"));
            }
            if (sessionId != null) {
                predicates.add(criteriaBuilder.equal(root.get("sessionId"), sessionId));
            }
            if (gateId != null) {
                predicates.add(criteriaBuilder.equal(root.get("gateId"), gateId));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
