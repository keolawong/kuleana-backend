package com.kahua.assets.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonBulkDto {
    private UUID orgId;

    private String firstName;

    private String lastName;

    private String notes;

    private String externalId;

    private boolean generateSticker;

    private UUID shiftId;

    private UUID gateId;

    private String organization;

    private String jobRole;

    private Boolean isActive;
}
