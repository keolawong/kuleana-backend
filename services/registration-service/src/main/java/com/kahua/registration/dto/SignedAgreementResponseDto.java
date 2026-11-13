package com.kahua.registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignedAgreementResponseDto {
    private UUID id;
    private UUID templateVersionId;
    private String participantId;
    private String participantName;
    private Boolean isMinor;
    private Boolean esignConsentAccepted;
    private String minorAssentName;
    private OffsetDateTime minorAssentAtUtc;
    private String signerName;
    private String signerRelationship;
    private String snapshotHash;
    private OffsetDateTime signedAtUtc;
}
