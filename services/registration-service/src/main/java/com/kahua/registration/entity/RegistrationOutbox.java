package com.kahua.registration.entity;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "registration_outbox", schema = "registration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false)
    private JsonNode payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "next_attempt_at_utc", nullable = false)
    private OffsetDateTime nextAttemptAtUtc;

    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private OffsetDateTime createdAtUtc;

    @Column(name = "processed_at_utc")
    private OffsetDateTime processedAtUtc;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAtUtc == null) createdAtUtc = now;
        if (nextAttemptAtUtc == null) nextAttemptAtUtc = now;
        if (status == null) status = OutboxStatus.PENDING;
        if (retryCount == null) retryCount = 0;
    }
}
