package com.kahua.registration.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "registration_forms",
    schema = "registration",
    uniqueConstraints = @UniqueConstraint(name = "uq_registration_forms_org_slug", columnNames = {"org_id", "slug"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationForm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(nullable = false, length = 150)
    private String slug;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "form_mode", nullable = false, length = 40)
    private FormMode formMode;

    @Column(name = "external_event_id", length = 255)
    private String externalEventId;

    @Column(name = "event_timezone", nullable = false, length = 100)
    @Builder.Default
    private String eventTimezone = "America/Denver";

    @Column(name = "go_live_at_utc")
    private OffsetDateTime goLiveAtUtc;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private OffsetDateTime createdAtUtc;

    @Column(name = "updated_at_utc", nullable = false)
    private OffsetDateTime updatedAtUtc;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAtUtc == null) createdAtUtc = now;
        if (updatedAtUtc == null) updatedAtUtc = now;
        if (isActive == null) isActive = true;
        if (eventTimezone == null || eventTimezone.isBlank()) eventTimezone = "America/Denver";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAtUtc = OffsetDateTime.now();
    }
}
