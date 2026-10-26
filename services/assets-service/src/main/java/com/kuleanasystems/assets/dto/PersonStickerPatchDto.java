package com.kuleanasystems.assets.dto;

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
public class PersonStickerPatchDto {
    private UUID personId;
    private UUID shiftId;
    private UUID gateId;

    @JsonIgnore
    private final Set<String> dirtyFields = new HashSet<>();

    public void setPersonId(UUID personId) {
        this.personId = personId;
        this.dirtyFields.add("personId");
    }

    public void setShiftId(UUID shiftId) {
        this.shiftId = shiftId;
        this.dirtyFields.add("shiftId");
    }

    public void setGateId(UUID gateId) {
        this.gateId = gateId;
        this.dirtyFields.add("gateId");
    }

    @JsonIgnore
    public boolean isDirty(String fieldName) {
        return dirtyFields.contains(fieldName);
    }
}
