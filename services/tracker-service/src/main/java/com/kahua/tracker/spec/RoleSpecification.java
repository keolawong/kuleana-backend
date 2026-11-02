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

    public static Specification<Role> hasAppName(String appName) {
        return (root, query, criteriaBuilder) -> 
            appName == null ? null : criteriaBuilder.equal(criteriaBuilder.lower(root.get("appName")), appName.toLowerCase());
    }

    public static Specification<Role> hasAppNameIn(java.util.Collection<String> appNames) {
        return (root, query, criteriaBuilder) -> {
            if (appNames == null || appNames.isEmpty()) return null;
            var inClause = criteriaBuilder.in(criteriaBuilder.lower(root.get("appName")));
            for (String app : appNames) {
                inClause.value(app.toLowerCase());
            }
            return inClause;
        };
    }
}
