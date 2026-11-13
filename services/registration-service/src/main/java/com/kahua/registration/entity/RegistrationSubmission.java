package com.kahua.registration.entity;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "registration_submissions", schema = "registration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_id", nullable = false)
    private RegistrationForm form;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_version_id", nullable = false)
    private RegistrationFormVersion formVersion;

    @Column(name = "account_user_id", nullable = false, length = 255)
    private String accountUserId;

    @Column(name = "participant_key", nullable = false, length = 255)
    private String participantKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "participant_profile_json", nullable = false)
    private JsonNode participantProfileJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers_json", nullable = false)
    private JsonNode answersJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "admin_fields_json", nullable = false)
    private JsonNode adminFieldsJson;

    @Column(name = "applied_window_id", length = 128)
    private String appliedWindowId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fee_snapshot_json", nullable = false)
    private JsonNode feeSnapshotJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 32)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.NOT_REQUIRED;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 32)
    @Builder.Default
    private SyncStatus syncStatus = SyncStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.DRAFT;

    @Column(name = "confirmation_code", length = 32)
    private String confirmationCode;

    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private OffsetDateTime createdAtUtc;

    @Column(name = "updated_at_utc", nullable = false)
    private OffsetDateTime updatedAtUtc;

    @Column(name = "submitted_at_utc")
    private OffsetDateTime submittedAtUtc;

    @Column(name = "cancelled_at_utc")
    private OffsetDateTime cancelledAtUtc;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAtUtc == null) createdAtUtc = now;
        if (updatedAtUtc == null) updatedAtUtc = now;
        if (paymentStatus == null) paymentStatus = PaymentStatus.NOT_REQUIRED;
        if (syncStatus == null) syncStatus = SyncStatus.PENDING;
        if (status == null) status = SubmissionStatus.DRAFT;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAtUtc = OffsetDateTime.now();
    }
}
