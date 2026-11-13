package com.kahua.registration.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "signed_agreements", schema = "registration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignedAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private RegistrationSubmission submission;

    @Column(name = "participant_id", nullable = false, length = 255)
    private String participantId;

    @Column(name = "participant_name", nullable = false, length = 255)
    private String participantName;

    @Column(name = "is_minor", nullable = false)
    private Boolean isMinor;

    @Column(name = "esign_consent_accepted", nullable = false)
    private Boolean esignConsentAccepted;

    @Column(name = "minor_assent_name", length = 255)
    private String minorAssentName;

    @Column(name = "minor_assent_at_utc")
    private OffsetDateTime minorAssentAtUtc;

    @Column(name = "signer_name", nullable = false, length = 255)
    private String signerName;

    @Column(name = "signer_relationship", length = 100)
    private String signerRelationship;

    @Column(name = "signature_data", nullable = false, columnDefinition = "TEXT")
    private String signatureData;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_version_id", nullable = false)
    private AgreementTemplate templateVersion;

    @Column(name = "snapshot_hash", nullable = false, length = 64)
    private String snapshotHash;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "signed_at_utc", nullable = false, updatable = false)
    private OffsetDateTime signedAtUtc;

    @PrePersist
    protected void onCreate() {
        if (signedAtUtc == null) signedAtUtc = OffsetDateTime.now();
    }
}
