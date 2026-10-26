package com.kuleanasystems.auth.repository;

import com.kuleanasystems.auth.entity.UserPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserPageRepository extends JpaRepository<UserPage, UUID> {

    List<UserPage> findByUserId(UUID userId);
}
