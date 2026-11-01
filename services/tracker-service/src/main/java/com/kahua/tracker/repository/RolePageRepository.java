package com.kahua.tracker.repository;

import com.kahua.tracker.entity.RolePage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RolePageRepository extends JpaRepository<RolePage, RolePage.RolePageId>, JpaSpecificationExecutor<RolePage> {
    List<RolePage> findByRoleId(UUID roleId);
}
