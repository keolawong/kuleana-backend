package com.kuleanasystems.assets.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GatePatchDto {
    private String name;
    private Boolean isActive;
    private Integer cashBanksRequiredForBusy;
    private Integer cashBanksRequiredForModerate;
    private Integer cashBanksRequiredForLite;
    private Integer displayOrder;

    @JsonIgnore
    private final Set<String> dirtyFields = new HashSet<>();

    public void setName(String name) {
        this.name = name;
        this.dirtyFields.add("name");
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        this.dirtyFields.add("isActive");
    }

    public void setCashBanksRequiredForBusy(Integer cashBanksRequiredForBusy) {
        this.cashBanksRequiredForBusy = cashBanksRequiredForBusy;
        this.dirtyFields.add("cashBanksRequiredForBusy");
    }

    public void setCashBanksRequiredForModerate(Integer cashBanksRequiredForModerate) {
        this.cashBanksRequiredForModerate = cashBanksRequiredForModerate;
        this.dirtyFields.add("cashBanksRequiredForModerate");
    }

    public void setCashBanksRequiredForLite(Integer cashBanksRequiredForLite) {
        this.cashBanksRequiredForLite = cashBanksRequiredForLite;
        this.dirtyFields.add("cashBanksRequiredForLite");
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
        this.dirtyFields.add("displayOrder");
    }

    @JsonIgnore
    public boolean isDirty(String fieldName) {
        return dirtyFields.contains(fieldName);
    }
}