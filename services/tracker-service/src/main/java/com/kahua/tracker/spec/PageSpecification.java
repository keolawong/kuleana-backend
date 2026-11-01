package com.kahua.tracker.spec;

import com.kahua.tracker.entity.Page;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class PageSpecification {

    public static Specification<Page> hasId(UUID id) {
        return (root, query, cb) -> id == null ? null : cb.equal(root.get("id"), id);
    }

    public static Specification<Page> hasPath(String path) {
        return (root, query, cb) -> path == null ? null : cb.like(cb.lower(root.get("path")), "%" + path.toLowerCase() + "%");
    }

    public static Specification<Page> hasName(String name) {
        return (root, query, cb) -> name == null ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Page> hasDescription(String description) {
        return (root, query, cb) -> description == null ? null : cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%");
    }
    
    public static Specification<Page> hasCreatedBy(String createdBy) {
        return (root, query, cb) -> createdBy == null ? null : cb.equal(root.get("createdBy"), createdBy);
    }
    
    public static Specification<Page> hasUpdatedBy(String updatedBy) {
        return (root, query, cb) -> updatedBy == null ? null : cb.equal(root.get("updatedBy"), updatedBy);
    }
}
