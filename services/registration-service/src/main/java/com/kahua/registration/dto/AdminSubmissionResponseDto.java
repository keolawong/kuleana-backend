package com.kahua.registration.dto;

import tools.jackson.databind.JsonNode;
import com.kahua.registration.entity.PaymentStatus;
import com.kahua.registration.entity.SubmissionStatus;
import com.kahua.registration.entity.SyncStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSubmissionResponseDto {
    private UUID id;
    private UUID orgId;
    private UUID formId;
    private UUID formVersionId;
    private String accountUserId;
    private String participantKey;
    private JsonNode participantProfileJson;
    private JsonNode answersJson;
    private JsonNode adminFieldsJson;
    private String appliedWindowId;
    private JsonNode feeSnapshotJson;
    private PaymentStatus paymentStatus;
    private SyncStatus syncStatus;
    private SubmissionStatus status;
    private String confirmationCode;
    private List<SelectionResponseDto> selections;
    private List<SignedAgreementResponseDto> signedAgreements;
    private OffsetDateTime createdAtUtc;
    private OffsetDateTime updatedAtUtc;
    private OffsetDateTime submittedAtUtc;
    private OffsetDateTime cancelledAtUtc;
}
