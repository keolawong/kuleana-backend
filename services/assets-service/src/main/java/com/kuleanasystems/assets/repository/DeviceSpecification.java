package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.Device;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class DeviceSpecification {

    public static Specification<Device> search(String search, Boolean isActive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

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
