package com.kahua.registration.repository;

import com.kahua.registration.entity.RegistrationOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RegistrationOptionRepository extends JpaRepository<RegistrationOption, UUID> {

    List<RegistrationOption> findByFormVersionId(UUID formVersionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RegistrationOption o SET o.reservedCount = o.reservedCount + 1, o.version = o.version + 1 " +
           "WHERE o.id = :optionId AND (o.maxCapacity IS NULL OR o.reservedCount < o.maxCapacity)")
    int reserveCapacity(@Param("optionId") UUID optionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RegistrationOption o SET o.reservedCount = o.reservedCount - 1, o.version = o.version + 1 " +
           "WHERE o.id = :optionId AND o.reservedCount > 0")
    int releaseCapacity(@Param("optionId") UUID optionId);
}
