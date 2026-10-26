package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.CashBank;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AssetCashBankSpecification {

    public static Specification<CashBank> searchForAsset(UUID operationId, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (operationId != null) {
                predicates.add(cb.equal(root.join("operation").get("operationId"), operationId));
            }

            if (search != null && !search.trim().isEmpty()) {
                String term = search.trim();
                List<Predicate> orPredicates = new ArrayList<>();

                // Case-insensitive match on bankName (exact or substring)
                orPredicates.add(cb.equal(cb.lower(root.get("bankName")), term.toLowerCase()));
                orPredicates.add(cb.like(cb.lower(root.get("bankName")), "%" + term.toLowerCase() + "%"));

                try {
                    UUID searchUuid = UUID.fromString(term);
                    orPredicates.add(cb.equal(root.get("cashBankId"), searchUuid));
                } catch (IllegalArgumentException e) {
                    // Ignore if not a valid UUID
                }

                predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}