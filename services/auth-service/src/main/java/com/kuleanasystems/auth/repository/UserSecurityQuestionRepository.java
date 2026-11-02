package com.kuleanasystems.auth.repository;

import com.kuleanasystems.auth.entity.UserSecurityQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserSecurityQuestionRepository extends JpaRepository<UserSecurityQuestion, UUID> {
    List<UserSecurityQuestion> findByUserId(UUID userId);
}
