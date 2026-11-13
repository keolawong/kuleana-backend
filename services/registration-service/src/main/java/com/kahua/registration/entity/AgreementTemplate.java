package com.kahua.registration.entity;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(
    name = "agreement_templates",
    schema = "registration",
    uniqueConstraints = @UniqueConstraint(name = "uq_agreement_templates_org_key_ver", columnNames = {"org_id", "template_key", "version"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "template_key", nullable = false, length = 128)
    private String templateKey;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, length = 64)
    private String type;

    @Column(name = "age_rule", nullable = false, length = 32)
    @Builder.Default
    private String ageRule = "ALL_AGES";

    @Column(name = "requirement_mode", nullable = false, length = 32)
    @Builder.Default
    private String requirementMode = "REQUIRED";

    @Column(nullable = false, length = 255)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary_bullets_json", nullable = false)
    private JsonNode summaryBulletsJson;

    @Column(name = "body_html", nullable = false, columnDefinition = "TEXT")
    private String bodyHtml;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        if (ageRule == null) ageRule = "ALL_AGES";
        if (requirementMode == null) requirementMode = "REQUIRED";
        if (isActive == null) isActive = true;
    }
}
