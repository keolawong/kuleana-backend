package com.kuleanasystems.assets.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GateCaptainDto {
    @NotBlank(message = "Captain name is required")
    private String captainName;

    @NotNull(message = "Gate ID is required")
    private UUID gateId;

    @NotNull(message = "Operation ID is required")
    private UUID operationId;

    @NotNull(message = "Shift ID is required")
    private UUID shiftId;
}