package com.kahua.registration.repository;

import com.kahua.registration.entity.FormAccountQuota;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FormAccountQuotaRepository extends JpaRepository<FormAccountQuota, UUID> {

    Optional<FormAccountQuota> findByFormIdAndAccountUserId(UUID formId, String accountUserId);

    List<FormAccountQuota> findByFormId(UUID formId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM FormAccountQuota q WHERE q.form.id = :formId AND q.accountUserId = :accountUserId")
    Optional<FormAccountQuota> findForUpdate(@Param("formId") UUID formId, @Param("accountUserId") String accountUserId);
}
