package com.kahua.registration.entity;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "registration_options", schema = "registration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_version_id", nullable = false)
    private RegistrationFormVersion formVersion;

    @Column(name = "external_ref_id", length = 255)
    private String externalRefId;

    @Column(name = "option_type", nullable = false, length = 64)
    private String optionType;

    @Column(nullable = false, length = 255)
    private String label;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", nullable = false)
    private JsonNode metadataJson;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column(name = "reserved_count", nullable = false)
    @Builder.Default
    private Integer reservedCount = 0;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        if (reservedCount == null) reservedCount = 0;
        if (version == null) version = 0L;
    }
}
