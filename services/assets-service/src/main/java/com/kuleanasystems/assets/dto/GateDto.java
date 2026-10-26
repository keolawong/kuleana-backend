package com.kuleanasystems.assets.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GateDto {
    @NotBlank(message = "Name is required")
    private String name;

    private Boolean isActive;

    private Integer cashBanksRequiredForBusy;

    private Integer cashBanksRequiredForModerate;

    private Integer cashBanksRequiredForLite;

    private Integer displayOrder;
}