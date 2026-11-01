package com.kahua.tracker.spec;

import com.kahua.tracker.entity.Role;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class RoleSpecification {

    public static Specification<Role> hasId(UUID id) {
        return (root, query, criteriaBuilder) -> 
            id == null ? null : criteriaBuilder.equal(root.get("id"), id);
    }

    public static Specification<Role> hasName(String name) {
        return (root, query, criteriaBuilder) -> 
            name == null ? null : criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }
    
    public static Specification<Role> hasIsActive(Boolean isActive) {
        return (root, query, criteriaBuilder) -> 
            isActive == null ? null : criteriaBuilder.equal(root.get("isActive"), isActive);
    }
}
