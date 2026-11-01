package com.kahua.assets.service;

import com.kahua.assets.dto.ShiftSalesDto;
import com.kahua.assets.dto.ShiftWorkerDto;
import com.kahua.assets.dto.WindowSalesDto;
import com.kahua.assets.entity.Shift;
import com.kahua.assets.entity.ShiftSales;
import com.kahua.assets.entity.ShiftWorker;
import com.kahua.assets.entity.WindowSales;
import com.kahua.assets.repository.ShiftRepository;
import com.kahua.assets.repository.ShiftSalesRepository;
import com.kahua.assets.repository.ShiftWorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesTrackerServiceTest {

    @Mock
    private ShiftSalesRepository shiftSalesRepository;

    @Mock
    private ShiftWorkerRepository shiftWorkerRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @InjectMocks
    private SalesTrackerService salesTrackerService;

    private UUID shiftId;
    private Shift shift;

    @BeforeEach
    void setUp() {
        shiftId = UUID.randomUUID();
        shift = new Shift();
        shift.setShiftId(shiftId);
        shift.setName("Morning Shift");
    }

    @Test
    void testGetShiftSales() {
        ShiftSales ss = new ShiftSales();
        ss.setShiftSalesId(UUID.randomUUID());
        ss.setShift(shift);
        ss.setRecordedAt(Instant.now());
        ss.setTotalSales(new BigDecimal("1500.00"));
        ss.setLaborCost(new BigDecimal("350.00"));

        WindowSales ws = new WindowSales();
        ws.setWindowSalesId(UUID.randomUUID());
        ws.setShiftSales(ss);
        ws.setWindowName("Gate 1 Window 1");
        ws.setSalesAmount(new BigDecimal("1500.00"));
        ss.setWindowSales(List.of(ws));

        when(shiftSalesRepository.findByShift_ShiftIdOrderByRecordedAtAsc(shiftId))
                .thenReturn(List.of(ss));

        List<ShiftSalesDto> results = salesTrackerService.getShiftSales(shiftId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTotalSales()).isEqualByComparingTo("1500.00");
        assertThat(results.get(0).getWindowSales()).hasSize(1);
        assertThat(results.get(0).getWindowSales().get(0).getWindowName()).isEqualTo("Gate 1 Window 1");
    }

    @Test
    void testSaveShiftSales() {
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(shift));

        ShiftSales savedEntity = new ShiftSales();
        savedEntity.setShiftSalesId(UUID.randomUUID());
        savedEntity.setShift(shift);
        savedEntity.setRecordedAt(Instant.now());
        savedEntity.setTotalSales(new BigDecimal("2400.00"));
        savedEntity.setLaborCost(new BigDecimal("420.00"));

        WindowSales ws = new WindowSales();
        ws.setWindowSalesId(UUID.randomUUID());
        ws.setShiftSales(savedEntity);
        ws.setWindowName("Gate 2 Window 1");
        ws.setSalesAmount(new BigDecimal("2400.00"));
        savedEntity.setWindowSales(List.of(ws));

        when(shiftSalesRepository.save(any(ShiftSales.class))).thenReturn(savedEntity);

        ShiftSalesDto inputDto = ShiftSalesDto.builder()
                .shiftId(shiftId)
                .recordedAt(Instant.now())
                .totalSales(new BigDecimal("2400.00"))
                .laborCost(new BigDecimal("420.00"))
                .windowSales(List.of(
                        WindowSalesDto.builder().windowName("Gate 2 Window 1").salesAmount(new BigDecimal("2400.00")).build()
                ))
                .build();

        ShiftSalesDto result = salesTrackerService.saveShiftSales(shiftId, inputDto);

        assertThat(result.getShiftSalesId()).isNotNull();
        assertThat(result.getTotalSales()).isEqualByComparingTo("2400.00");
        verify(shiftSalesRepository).save(any(ShiftSales.class));
    }

    @Test
    void testGetShiftWorkers_ReturnsExisting() {
        ShiftWorker sw = new ShiftWorker();
        sw.setShiftWorkerId(UUID.randomUUID());
        sw.setShift(shift);
        sw.setRoleName("Lead Cashier");
        sw.setHourlyRate(new BigDecimal("18.50"));
        sw.setCount(3);

        when(shiftWorkerRepository.findByShift_ShiftIdOrderByRoleNameAsc(shiftId))
                .thenReturn(List.of(sw));

        List<ShiftWorkerDto> workers = salesTrackerService.getShiftWorkers(shiftId);

        assertThat(workers).hasSize(1);
        assertThat(workers.get(0).getRoleName()).isEqualTo("Lead Cashier");
        assertThat(workers.get(0).getCount()).isEqualTo(3);
    }

    @Test
    void testGetShiftWorkers_SeedsDefaultsWhenEmpty() {
        when(shiftWorkerRepository.findByShift_ShiftIdOrderByRoleNameAsc(shiftId))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(shift));

        when(shiftWorkerRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<ShiftWorker> list = invocation.getArgument(0);
            list.forEach(w -> w.setShiftWorkerId(UUID.randomUUID()));
            return list;
        });

        List<ShiftWorkerDto> workers = salesTrackerService.getShiftWorkers(shiftId);

        assertThat(workers).hasSize(4);
        assertThat(workers).extracting(ShiftWorkerDto::getRoleName)
                .containsExactlyInAnyOrder("Cashier", "Gate Lead", "Supervisor", "Volunteer");
        verify(shiftWorkerRepository).saveAll(anyList());
    }

    @Test
    void testUpdateShiftWorkers() {
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(shift));

        List<ShiftWorkerDto> updatePayload = List.of(
                ShiftWorkerDto.builder().roleName("Cashier").hourlyRate(new BigDecimal("16.00")).count(5).build(),
                ShiftWorkerDto.builder().roleName("Lead").hourlyRate(new BigDecimal("22.00")).count(2).build()
        );

        when(shiftWorkerRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<ShiftWorker> list = invocation.getArgument(0);
            list.forEach(w -> w.setShiftWorkerId(UUID.randomUUID()));
            return list;
        });

        List<ShiftWorkerDto> result = salesTrackerService.updateShiftWorkers(shiftId, updatePayload);

        assertThat(result).hasSize(2);
        verify(shiftWorkerRepository).deleteByShift_ShiftId(shiftId);
        verify(shiftWorkerRepository).saveAll(anyList());
    }

    @Test
    void testResetShiftSales() {
        salesTrackerService.resetShiftSales(shiftId);
        verify(shiftSalesRepository).deleteByShift_ShiftId(shiftId);
    }

    @Test
    void testRemoveWindowSales() {
        ShiftSales ss = new ShiftSales();
        ss.setShiftSalesId(UUID.randomUUID());
        ss.setShift(shift);
        ss.setTotalSales(new BigDecimal("3000.00"));

        WindowSales ws1 = new WindowSales();
        ws1.setWindowName("Gate 1 Window 1");
        ws1.setSalesAmount(new BigDecimal("1000.00"));

        WindowSales ws2 = new WindowSales();
        ws2.setWindowName("Gate 1 Window 2");
        ws2.setSalesAmount(new BigDecimal("2000.00"));

        ss.setWindowSales(new ArrayList<>(List.of(ws1, ws2)));

        when(shiftSalesRepository.findByShift_ShiftIdOrderByRecordedAtAsc(shiftId))
                .thenReturn(List.of(ss));

        salesTrackerService.removeWindowSales(shiftId, "Gate 1 Window 2");

        assertThat(ss.getWindowSales()).hasSize(1);
        assertThat(ss.getWindowSales().get(0).getWindowName()).isEqualTo("Gate 1 Window 1");
        assertThat(ss.getTotalSales()).isEqualByComparingTo("1000.00");
        verify(shiftSalesRepository).saveAll(List.of(ss));
        verify(shiftSalesRepository).flush();
    }

    @Test
    void testRemoveGateSales() {
        ShiftSales ss = new ShiftSales();
        ss.setShiftSalesId(UUID.randomUUID());
        ss.setShift(shift);
        ss.setTotalSales(new BigDecimal("5000.00"));

        WindowSales ws1 = new WindowSales();
        ws1.setWindowName("Gate 1 Window 1");
        ws1.setSalesAmount(new BigDecimal("1000.00"));

        WindowSales ws2 = new WindowSales();
        ws2.setWindowName("Gate 1 Window 2");
        ws2.setSalesAmount(new BigDecimal("1500.00"));

        WindowSales ws3 = new WindowSales();
        ws3.setWindowName("Gate 2 Window 1");
        ws3.setSalesAmount(new BigDecimal("2500.00"));

        ss.setWindowSales(new ArrayList<>(List.of(ws1, ws2, ws3)));

        when(shiftSalesRepository.findByShift_ShiftIdOrderByRecordedAtAsc(shiftId))
                .thenReturn(List.of(ss));

        salesTrackerService.removeGateSales(shiftId, "Gate 1");

        assertThat(ss.getWindowSales()).hasSize(1);
        assertThat(ss.getWindowSales().get(0).getWindowName()).isEqualTo("Gate 2 Window 1");
        assertThat(ss.getTotalSales()).isEqualByComparingTo("2500.00");
        verify(shiftSalesRepository).saveAll(List.of(ss));
        verify(shiftSalesRepository).flush();
    }

    @Test
    void testRemoveGateSales_withBareGateIdentifier() {
        ShiftSales ss = new ShiftSales();
        ss.setShiftSalesId(UUID.randomUUID());
        ss.setShift(shift);
        ss.setTotalSales(new BigDecimal("3500.00"));

        WindowSales ws1 = new WindowSales();
        ws1.setWindowName("Gate 2 Window 1");
        ws1.setSalesAmount(new BigDecimal("1000.00"));

        WindowSales ws2 = new WindowSales();
        ws2.setWindowName("Gate 3 Window 1");
        ws2.setSalesAmount(new BigDecimal("2500.00"));

        ss.setWindowSales(new ArrayList<>(List.of(ws1, ws2)));

        when(shiftSalesRepository.findByShift_ShiftIdOrderByRecordedAtAsc(shiftId))
                .thenReturn(List.of(ss));

        // Calling with bare gate number "2" should remove "Gate 2 Window 1"
        salesTrackerService.removeGateSales(shiftId, "2");

        assertThat(ss.getWindowSales()).hasSize(1);
        assertThat(ss.getWindowSales().get(0).getWindowName()).isEqualTo("Gate 3 Window 1");
        assertThat(ss.getTotalSales()).isEqualByComparingTo("2500.00");
        verify(shiftSalesRepository).saveAll(List.of(ss));
        verify(shiftSalesRepository).flush();
    }
}
