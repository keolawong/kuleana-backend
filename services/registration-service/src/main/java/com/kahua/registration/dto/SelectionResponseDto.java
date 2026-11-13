package com.kahua.registration.dto;

import com.kahua.registration.entity.SelectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectionResponseDto {
    private UUID id;
    private UUID optionId;
    private String externalRefId;
    private String optionType;
    private String label;
    private SelectionStatus status;
}
