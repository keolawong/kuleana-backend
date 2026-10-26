package com.kuleanasystems.assets.dto.report;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationSectionRowDto {
    private int sessionNum;
    private String day;
    private String time;
    private Map<String, OperationTotalsDto> data;
}
