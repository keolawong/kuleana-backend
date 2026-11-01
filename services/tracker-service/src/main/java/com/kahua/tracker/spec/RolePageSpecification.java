package com.kahua.tracker.spec;

import com.kahua.tracker.entity.RolePage;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class RolePageSpecification {

    public static Specification<RolePage> hasRoleId(UUID roleId) {
        return (root, query, cb) -> roleId == null ? null : cb.equal(root.get("roleId"), roleId);
    }

    public static Specification<RolePage> hasPageId(UUID pageId) {
        return (root, query, cb) -> pageId == null ? null : cb.equal(root.get("pageId"), pageId);
    }

    public static Specification<RolePage> hasIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null ? null : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<RolePage> hasCreatedBy(String createdBy) {
        return (root, query, cb) -> createdBy == null ? null : cb.equal(root.get("createdBy"), createdBy);
    }
    
    public static Specification<RolePage> hasUpdatedBy(String updatedBy) {
        return (root, query, cb) -> updatedBy == null ? null : cb.equal(root.get("updatedBy"), updatedBy);
    }
}
