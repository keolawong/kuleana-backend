package com.kuleanasystems.assets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonBulkWorkerDto {
    private String firstName;
    private String lastName;
    private String organization;
    private String jobRole;
    private UUID gateId;
    private String gateName;
    private String externalId;
    private String notes;
}
