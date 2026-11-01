package com.kahua.tracker.spec;

import com.kahua.tracker.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class UserSpecification {

    public static Specification<User> hasId(UUID id) {
        return (root, query, criteriaBuilder) -> 
            id == null ? null : criteriaBuilder.equal(root.get("id"), id);
    }

    public static Specification<User> hasUsername(String username) {
        return (root, query, criteriaBuilder) -> 
            username == null ? null : criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), "%" + username.toLowerCase() + "%");
    }
}
