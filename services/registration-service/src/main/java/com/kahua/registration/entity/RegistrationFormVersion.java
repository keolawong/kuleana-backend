package com.kahua.registration.entity;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "registration_form_versions",
    schema = "registration",
    uniqueConstraints = @UniqueConstraint(name = "uq_registration_form_versions_form_version", columnNames = {"form_id", "version"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationFormVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_id", nullable = false)
    private RegistrationForm form;

    @Column(nullable = false)
    private Integer version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_json", nullable = false)
    private JsonNode schemaJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "logic_rules_json", nullable = false)
    private JsonNode logicRulesJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", nullable = false)
    private JsonNode configJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cutoff_windows_json", nullable = false)
    private JsonNode cutoffWindowsJson;

    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private OffsetDateTime createdAtUtc;

    @PrePersist
    protected void onCreate() {
        if (createdAtUtc == null) createdAtUtc = OffsetDateTime.now();
    }
}
