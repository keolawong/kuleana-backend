package com.kahua.assets.repository;

import com.kahua.assets.entity.Device;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeviceSpecification {

    public static Specification<Device> search(String search, Boolean isActive) {
        return search(search, isActive, null);
    }

    public static Specification<Device> search(String search, Boolean isActive, UUID orgId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (orgId != null) {
                predicates.add(cb.equal(root.get("orgId"), orgId));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            if (search != null && !search.trim().isEmpty()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Predicate matchDeviceId = cb.like(cb.lower(root.get("deviceId")), term);
                Predicate matchDeviceNumber = cb.like(cb.lower(root.get("deviceNumber")), term);
                predicates.add(cb.or(matchDeviceId, matchDeviceNumber));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
