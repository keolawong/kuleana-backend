package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.dto.AssetSearchResponseDto;
import com.kuleanasystems.assets.dto.CashBankDto;
import com.kuleanasystems.assets.dto.DeviceDto;
import com.kuleanasystems.assets.dto.TicketBookDto;
import com.kuleanasystems.assets.entity.CashBank;
import com.kuleanasystems.assets.entity.Device;
import com.kuleanasystems.assets.entity.TicketBook;
import com.kuleanasystems.assets.repository.AssetCashBankSpecification;
import com.kuleanasystems.assets.repository.AssetTicketBookSpecification;
import com.kuleanasystems.assets.repository.CashBankRepository;
import com.kuleanasystems.assets.repository.DeviceRepository;
import com.kuleanasystems.assets.repository.DeviceSpecification;
import com.kuleanasystems.assets.repository.TicketBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final CashBankRepository cashBankRepository;
    private final TicketBookRepository ticketBookRepository;
    private final DeviceRepository deviceRepository;

    @Transactional(readOnly = true)
    public AssetSearchResponseDto searchAssets(UUID operationId, String search) {
        AssetSearchResponseDto response = new AssetSearchResponseDto();

        // Search CashBanks
        Specification<CashBank> cashBankSpec = AssetCashBankSpecification.searchForAsset(operationId, search);
        List<CashBank> cashBanks = cashBankRepository.findAll(cashBankSpec);
        response.setCashBank(cashBanks.stream().map(this::mapToCashBankDto).collect(Collectors.toList()));

        // Search TicketBooks
        Specification<TicketBook> ticketBookSpec = AssetTicketBookSpecification.searchForAsset(operationId, search);
        List<TicketBook> ticketBooks = ticketBookRepository.findAll(ticketBookSpec);
        response.setTicketBook(ticketBooks.stream().map(this::mapToTicketBookDto).collect(Collectors.toList()));

        // Search Devices (active)
        Specification<Device> deviceSpec = DeviceSpecification.search(search, true);
        List<Device> devices = deviceRepository.findAll(deviceSpec);
        response.setDevice(devices.stream().map(this::mapToDeviceDto).collect(Collectors.toList()));

        return response;
    }

    private CashBankDto mapToCashBankDto(CashBank cashBank) {
        CashBankDto dto = new CashBankDto();
        dto.setCashBankId(cashBank.getCashBankId());
        dto.setBankName(cashBank.getBankName());
        dto.setStartingAmount(cashBank.getStartingAmount());
        dto.setOfficialCount(cashBank.getOfficialCount());
        dto.setCount100s(cashBank.getCount100s());
        dto.setCount50s(cashBank.getCount50s());
        dto.setCount20s(cashBank.getCount20s());
        dto.setCount10s(cashBank.getCount10s());
        dto.setCount5s(cashBank.getCount5s());
        dto.setCount1s(cashBank.getCount1s());
        dto.setCount1cs(cashBank.getCount1cs());
        dto.setStatus(cashBank.getStatus());
        if (cashBank.getOperation() != null) dto.setOperationId(cashBank.getOperation().getOperationId());
        if (cashBank.getShift() != null) dto.setShiftId(cashBank.getShift().getShiftId());
        if (cashBank.getGate() != null) dto.setGateId(cashBank.getGate().getGateId());
        dto.setDeviceId(cashBank.getDeviceId());
        dto.setParentCashBankId(cashBank.getParentCashBankId());
        dto.setProcessId(cashBank.getProcessId());
        return dto;
    }

    private TicketBookDto mapToTicketBookDto(TicketBook ticketBook) {
        TicketBookDto dto = new TicketBookDto();
        dto.setTicketBookId(ticketBook.getTicketBookId());
        dto.setScanCode(ticketBook.getScanCode());
        dto.setStartNum(ticketBook.getStartNum());
        dto.setEndNum(ticketBook.getEndNum());
        dto.setNextTicket(ticketBook.getNextTicket());
        dto.setIsReusable(ticketBook.getIsReusable());
        dto.setStatus(ticketBook.getStatus());
        if (ticketBook.getOperation() != null) dto.setOperationId(ticketBook.getOperation().getOperationId());
        if (ticketBook.getShift() != null) dto.setShiftId(ticketBook.getShift().getShiftId());
        if (ticketBook.getGate() != null) dto.setGateId(ticketBook.getGate().getGateId());
        dto.setDeviceId(ticketBook.getDeviceId());
        dto.setParentTicketBookId(ticketBook.getParentTicketBookId());
        dto.setProcessId(ticketBook.getProcessId());
        return dto;
    }

    private DeviceDto mapToDeviceDto(Device device) {
        DeviceDto dto = new DeviceDto();
        dto.setId(device.getId());
        dto.setDeviceId(device.getDeviceId());
        dto.setDeviceNumber(device.getDeviceNumber());
        dto.setIsActive(device.getIsActive());
        dto.setCreatedBy(device.getCreatedBy());
        dto.setCreatedAt(device.getCreatedAt());
        dto.setUpdatedBy(device.getUpdatedBy());
        dto.setUpdatedAt(device.getUpdatedAt());
        return dto;
    }
}