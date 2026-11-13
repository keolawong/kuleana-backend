package com.kahua.registration.dto;

import com.kahua.registration.entity.FormMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class AdminFormCreateRequestDto {
    private UUID orgId;

    @NotBlank
    private String slug;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private FormMode formMode;

    private String externalEventId;

    private String eventTimezone;

    private OffsetDateTime goLiveAtUtc;

    private Boolean isActive;
}
