package com.kuleanasystems.assets.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonBulkDto {
    private String firstName;

    private String lastName;

    private String notes;

    private String externalId;

    private boolean generateSticker;

    private java.util.UUID shiftId;

    private java.util.UUID gateId;

    private String organization;

    private String jobRole;

    private Boolean isActive;
}
