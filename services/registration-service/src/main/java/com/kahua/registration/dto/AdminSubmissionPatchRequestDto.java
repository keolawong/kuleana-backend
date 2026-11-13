package com.kahua.registration.dto;

import tools.jackson.databind.JsonNode;
import com.kahua.registration.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSubmissionPatchRequestDto {
    private JsonNode adminFieldsJson;
    private PaymentStatus paymentStatus;
}
