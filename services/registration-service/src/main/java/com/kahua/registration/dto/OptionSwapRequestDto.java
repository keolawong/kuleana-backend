package com.kahua.registration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionSwapRequestDto {
    @NotNull
    private UUID oldOptionId;

    @NotNull
    private UUID newOptionId;

    private String expectedWindowId;
}
