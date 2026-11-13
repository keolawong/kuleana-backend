package com.kahua.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignedAgreementRequestDto {
    @NotNull
    private UUID templateVersionId;

    @NotBlank
    private String participantId;

    @NotBlank
    private String participantName;

    private String participantDob;

    @NotNull
    private Boolean esignConsentAccepted;

    @NotBlank
    private String signerName;

    private String signerRelationship;

    private Boolean guardianAuthorityAttested;

    private String minorAssentName;

    @NotBlank
    private String signatureData;

    private String expectedSnapshotHash;
}
