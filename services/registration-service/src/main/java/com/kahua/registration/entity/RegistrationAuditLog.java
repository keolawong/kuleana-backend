package com.kahua.registration.entity;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "registration_audit_log", schema = "registration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(name = "actor_user_id", nullable = false, length = 255)
    private String actorUserId;

    @Column(nullable = false, length = 100)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_json")
    private JsonNode beforeJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_json")
    private JsonNode afterJson;

    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private OffsetDateTime createdAtUtc;

    @PrePersist
    protected void onCreate() {
        if (createdAtUtc == null) createdAtUtc = OffsetDateTime.now();
    }
}
