package com.kuleanasystems.assets.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class AssetSearchResponseDto {
    @JsonProperty("cash-bank")
    private List<CashBankDto> cashBank;

    @JsonProperty("ticket-book")
    private List<TicketBookDto> ticketBook;

    @JsonProperty("device")
    private List<DeviceDto> device;
}