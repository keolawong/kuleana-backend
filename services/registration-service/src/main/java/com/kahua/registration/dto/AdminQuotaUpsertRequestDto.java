package com.kahua.registration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminQuotaUpsertRequestDto {
    private UUID orgId;

    @NotBlank
    private String accountUserId;

    private Integer overrideMaxSubmissions;
}
