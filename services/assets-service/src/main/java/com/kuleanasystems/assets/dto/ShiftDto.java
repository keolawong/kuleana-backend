package com.kuleanasystems.assets.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShiftDto {
    @NotNull(message = "Operation ID is required")
    private UUID operationId;

    @NotBlank(message = "Name is required")
    private String name;

    private Instant startTime;

    private Instant endTime;

    private String traffic;

    private String status;
}