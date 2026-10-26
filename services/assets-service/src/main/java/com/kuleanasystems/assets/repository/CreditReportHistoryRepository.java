package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.CreditReportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CreditReportHistoryRepository extends JpaRepository<CreditReportHistory, UUID> {
}
