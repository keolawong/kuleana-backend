package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.TicketBook;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AssetTicketBookSpecification {

    public static Specification<TicketBook> searchForAsset(UUID operationId, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (operationId != null) {
                predicates.add(cb.equal(root.join("operation").get("operationId"), operationId));
            }

            if (search != null && !search.trim().isEmpty()) {
                String term = search.trim();
                List<Predicate> orPredicates = new ArrayList<>();

                // Case-insensitive match on scanCode (exact or substring)
                orPredicates.add(cb.equal(cb.lower(root.get("scanCode")), term.toLowerCase()));
                orPredicates.add(cb.like(cb.lower(root.get("scanCode")), "%" + term.toLowerCase() + "%"));

                // Exact match on startNum or endNum
                try {
                    Integer searchInt = Integer.parseInt(term);
                    orPredicates.add(cb.equal(root.get("startNum"), searchInt));
                    orPredicates.add(cb.equal(root.get("endNum"), searchInt));
                } catch (NumberFormatException e) {
                    // Ignore if not a valid integer
                }

                try {
                    UUID searchUuid = UUID.fromString(term);
                    orPredicates.add(cb.equal(root.get("ticketBookId"), searchUuid));
                } catch (IllegalArgumentException e) {
                    // Ignore if not a valid UUID
                }

                predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}