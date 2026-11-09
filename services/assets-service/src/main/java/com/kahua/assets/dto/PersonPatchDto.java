package com.kahua.assets.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonPatchDto {
    private UUID orgId;
    private String firstName;
    private String lastName;
    private String notes;
    private String externalId;
    private UUID shiftId;
    private UUID gateId;
    private String organization;
    private String jobRole;
    private Boolean isActive;

    @JsonIgnore
    private final Set<String> dirtyFields = new HashSet<>();

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
        this.dirtyFields.add("orgId");
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        this.dirtyFields.add("firstName");
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.dirtyFields.add("lastName");
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.dirtyFields.add("notes");
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
        this.dirtyFields.add("externalId");
    }

    public void setShiftId(UUID shiftId) {
        this.shiftId = shiftId;
        this.dirtyFields.add("shiftId");
    }

    public void setGateId(UUID gateId) {
        this.gateId = gateId;
        this.dirtyFields.add("gateId");
    }

    public void setOrganization(String organization) {
        this.organization = organization;
        this.dirtyFields.add("organization");
    }

    public void setJobRole(String jobRole) {
        this.jobRole = jobRole;
        this.dirtyFields.add("jobRole");
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        this.dirtyFields.add("isActive");
    }

    @JsonIgnore
    public boolean isDirty(String fieldName) {
        return dirtyFields.contains(fieldName);
    }
}
