package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.dto.ShiftSalesDto;
import com.kuleanasystems.assets.dto.ShiftWorkerDto;
import com.kuleanasystems.assets.dto.WindowSalesDto;
import com.kuleanasystems.assets.entity.Shift;
import com.kuleanasystems.assets.entity.ShiftSales;
import com.kuleanasystems.assets.entity.ShiftWorker;
import com.kuleanasystems.assets.entity.WindowSales;
import com.kuleanasystems.assets.repository.ShiftRepository;
import com.kuleanasystems.assets.repository.ShiftSalesRepository;
import com.kuleanasystems.assets.repository.ShiftWorkerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesTrackerService {

    private final ShiftSalesRepository shiftSalesRepository;
    private final ShiftWorkerRepository shiftWorkerRepository;
    private final ShiftRepository shiftRepository;

    @Transactional(readOnly = true)
    public List<ShiftSalesDto> getShiftSales(UUID shiftId) {
        List<ShiftSales> list = shiftSalesRepository.findByShift_ShiftIdOrderByRecordedAtAsc(shiftId);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public ShiftSalesDto saveShiftSales(UUID shiftId, ShiftSalesDto dto) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + shiftId));

        ShiftSales entity = new ShiftSales();
        entity.setShift(shift);
        entity.setRecordedAt(dto.getRecordedAt() != null ? dto.getRecordedAt() : Instant.now());
        entity.setTotalSales(dto.getTotalSales() != null ? dto.getTotalSales() : BigDecimal.ZERO);
        entity.setLaborCost(dto.getLaborCost() != null ? dto.getLaborCost() : BigDecimal.ZERO);

        if (dto.getWindowSales() != null) {
            for (WindowSalesDto wDto : dto.getWindowSales()) {
                WindowSales ws = new WindowSales();
                ws.setShiftSales(entity);
                String winName = wDto.getWindowName() != null ? wDto.getWindowName().trim() : "Window 1";
                if (winName.length() > 60) {
                    winName = winName.substring(0, 60);
                }
                ws.setWindowName(winName.isEmpty() ? "Window 1" : winName);
                ws.setSalesAmount(wDto.getSalesAmount() != null ? wDto.getSalesAmount() : BigDecimal.ZERO);
                entity.getWindowSales().add(ws);
            }
        }

        ShiftSales saved = shiftSalesRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public List<ShiftWorkerDto> getShiftWorkers(UUID shiftId) {
        List<ShiftWorker> workers = shiftWorkerRepository.findByShift_ShiftIdOrderByRoleNameAsc(shiftId);
        if (workers.isEmpty()) {
            Shift shift = shiftRepository.findById(shiftId)
                    .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + shiftId));

            // Populate sensible field defaults: Cashier ($15.00, 4), Gate Lead ($20.00, 1), Supervisor ($25.00, 1), Volunteer ($0.00, 2)
            List<ShiftWorker> defaults = List.of(
                    createWorker(shift, "Cashier", new BigDecimal("15.00"), 4),
                    createWorker(shift, "Gate Lead", new BigDecimal("20.00"), 1),
                    createWorker(shift, "Supervisor", new BigDecimal("25.00"), 1),
                    createWorker(shift, "Volunteer", BigDecimal.ZERO, 2)
            );
            workers = shiftWorkerRepository.saveAll(defaults);
        }
        return workers.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public List<ShiftWorkerDto> updateShiftWorkers(UUID shiftId, List<ShiftWorkerDto> dtos) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + shiftId));

        shiftWorkerRepository.deleteByShift_ShiftId(shiftId);
        shiftWorkerRepository.flush();

        List<ShiftWorker> newWorkers = new ArrayList<>();
        if (dtos != null) {
            for (ShiftWorkerDto dto : dtos) {
                newWorkers.add(createWorker(
                        shift,
                        dto.getRoleName(),
                        dto.getHourlyRate() != null ? dto.getHourlyRate() : BigDecimal.ZERO,
                        dto.getCount() != null ? dto.getCount() : 0
                ));
            }
        }
        List<ShiftWorker> saved = shiftWorkerRepository.saveAll(newWorkers);
        return saved.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public void resetShiftSales(UUID shiftId) {
        shiftSalesRepository.deleteByShift_ShiftId(shiftId);
        shiftSalesRepository.flush();
    }

    @Transactional
    public void removeWindowSales(UUID shiftId, String windowName) {
        if (windowName == null || windowName.trim().isEmpty()) {
            return;
        }
        String targetName = windowName.trim();
        List<ShiftSales> snapshots = shiftSalesRepository.findByShift_ShiftIdOrderByRecordedAtAsc(shiftId);
        for (ShiftSales ss : snapshots) {
            boolean removed = ss.getWindowSales().removeIf(ws -> ws.getWindowName() != null && ws.getWindowName().equalsIgnoreCase(targetName));
            if (removed) {
                BigDecimal newTotal = ss.getWindowSales().stream()
                        .map(ws -> ws.getSalesAmount() != null ? ws.getSalesAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                ss.setTotalSales(newTotal);
            }
        }
        shiftSalesRepository.saveAll(snapshots);
        shiftSalesRepository.flush();
    }

    @Transactional
    public void removeGateSales(UUID shiftId, String gateName) {
        if (gateName == null || gateName.trim().isEmpty()) {
            return;
        }
        String rawTarget = gateName.trim().toLowerCase();
        String bareGate = rawTarget.replaceFirst("^(?i)gate\\s*", "");
        String withGate = "gate " + bareGate;
        List<ShiftSales> snapshots = shiftSalesRepository.findByShift_ShiftIdOrderByRecordedAtAsc(shiftId);
        for (ShiftSales ss : snapshots) {
            boolean removed = ss.getWindowSales().removeIf(ws -> {
                if (ws.getWindowName() == null) return false;
                String wName = ws.getWindowName().toLowerCase();
                return wName.startsWith(rawTarget + " ") || wName.startsWith(rawTarget + "-") || wName.equalsIgnoreCase(rawTarget)
                        || wName.startsWith(bareGate + " ") || wName.startsWith(bareGate + "-") || wName.equalsIgnoreCase(bareGate)
                        || wName.startsWith(withGate + " ") || wName.startsWith(withGate + "-") || wName.equalsIgnoreCase(withGate);
            });
            if (removed) {
                BigDecimal newTotal = ss.getWindowSales().stream()
                        .map(ws -> ws.getSalesAmount() != null ? ws.getSalesAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                ss.setTotalSales(newTotal);
            }
        }
        shiftSalesRepository.saveAll(snapshots);
        shiftSalesRepository.flush();
    }

    private ShiftWorker createWorker(Shift shift, String roleName, BigDecimal rate, int count) {
        ShiftWorker sw = new ShiftWorker();
        sw.setShift(shift);
        String name = roleName != null ? roleName.trim() : "Worker";
        if (name.length() > 50) {
            name = name.substring(0, 50);
        }
        sw.setRoleName(name.isEmpty() ? "Worker" : name);
        sw.setHourlyRate(rate != null ? rate : BigDecimal.ZERO);
        sw.setCount(Math.max(0, count));
        return sw;
    }

    private ShiftSalesDto toDto(ShiftSales entity) {
        List<WindowSalesDto> wDtos = entity.getWindowSales().stream().map(ws -> WindowSalesDto.builder()
                .windowSalesId(ws.getWindowSalesId())
                .windowName(ws.getWindowName())
                .salesAmount(ws.getSalesAmount())
                .build()).collect(Collectors.toList());

        return ShiftSalesDto.builder()
                .shiftSalesId(entity.getShiftSalesId())
                .shiftId(entity.getShift().getShiftId())
                .recordedAt(entity.getRecordedAt())
                .totalSales(entity.getTotalSales())
                .laborCost(entity.getLaborCost())
                .windowSales(wDtos)
                .build();
    }

    private ShiftWorkerDto toDto(ShiftWorker sw) {
        return ShiftWorkerDto.builder()
                .shiftWorkerId(sw.getShiftWorkerId())
                .shiftId(sw.getShift().getShiftId())
                .roleName(sw.getRoleName())
                .hourlyRate(sw.getHourlyRate())
                .count(sw.getCount())
                .build();
    }
}
