package com.kahua.tracker.repository;

import com.kahua.tracker.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PageRepository extends JpaRepository<Page, UUID>, JpaSpecificationExecutor<Page> {
}
