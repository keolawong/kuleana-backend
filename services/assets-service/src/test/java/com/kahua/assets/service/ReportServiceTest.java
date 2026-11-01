package com.kahua.assets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.kahua.assets.dto.report.*;
import com.kahua.assets.entity.*;
import com.kahua.assets.repository.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private OperationRepository operationRepository;

    @Mock
    private GateRepository gateRepository;

    @Mock
    private CashBankRepository cashBankRepository;

    @Mock
    private TicketBookRepository ticketBookRepository;

    @Mock
    private CreditReportRepository creditReportRepository;

    @InjectMocks
    private ReportService reportService;

    private UUID operationId;
    private UUID shiftId;
    private UUID gateId;
    private Operation operation;
    private Shift shift;
    private Gate gate;

    @BeforeEach
    void setUp() {
        operationId = UUID.randomUUID();
        shiftId = UUID.randomUUID();
        gateId = UUID.randomUUID();

        operation = new Operation();
        operation.setOperationId(operationId);
        operation.setName("Balloon Fiesta 2026");
        operation.setTicketPrice(BigDecimal.valueOf(15.00));
        operation.setCcFee(BigDecimal.valueOf(1.45));

        shift = new Shift();
        shift.setShiftId(shiftId);
        shift.setName("Shift 1");
        shift.setOperation(operation);
        shift.setStartTime(Instant.now());
        shift.setEndTime(Instant.now().plusSeconds(3600));

        gate = new Gate();
        gate.setGateId(gateId);
        gate.setName("Gate 32");
        gate.setDisplayOrder(1);
    }

    @Test
    void getShiftReport_shouldCalculateCashAndCreditDevicesAccurately() {
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        when(gateRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc()).thenReturn(List.of(gate));

        // Cash Device: DEV-101
        CashBank bank1 = new CashBank();
        bank1.setShift(shift);
        bank1.setGate(gate);
        bank1.setDeviceId("DEV-101");
        bank1.setBankName("Bank 101");
        bank1.setStartingAmount(BigDecimal.valueOf(500.00));
        bank1.setOfficialCount(BigDecimal.valueOf(1250.00));
        bank1.setStatus("Returned");

        // Cash Bank on DEV-101 sharing identical official count (should deduplicate official count)
        CashBank bank2 = new CashBank();
        bank2.setShift(shift);
        bank2.setGate(gate);
        bank2.setDeviceId("DEV-101");
        bank2.setBankName("Bank 101-Supp");
        bank2.setStartingAmount(BigDecimal.valueOf(200.00));
        bank2.setOfficialCount(BigDecimal.valueOf(1250.00));
        bank2.setStatus("Returned");

        // Returned Unused Bank (should be excluded from device sales)
        CashBank unusedBank = new CashBank();
        unusedBank.setShift(shift);
        unusedBank.setGate(gate);
        unusedBank.setDeviceId("DEV-999");
        unusedBank.setStatus("Returned Unused");

        when(cashBankRepository.findByShift_ShiftId(shiftId)).thenReturn(List.of(bank1, bank2, unusedBank));

        // Ticket Book for Cash Device (DEV-101): 50 tickets sold
        TicketBook cashBook = new TicketBook();
        cashBook.setShift(shift);
        cashBook.setGate(gate);
        cashBook.setDeviceId("DEV-101");
        cashBook.setStartNum(100);
        cashBook.setEndNum(200);
        cashBook.setNextTicket(150);
        cashBook.setStatus("Returned");

        // Ticket Book for Credit Device (DEV-202): 40 tickets sold
        TicketBook creditBook = new TicketBook();
        creditBook.setShift(shift);
        creditBook.setGate(gate);
        creditBook.setDeviceId("DEV-202");
        creditBook.setStartNum(500);
        creditBook.setEndNum(600);
        creditBook.setNextTicket(540);
        creditBook.setStatus("COMPLETED");

        // Unused ticket book (0 tickets sold: nextTicket == startNum)
        TicketBook unusedBook = new TicketBook();
        unusedBook.setShift(shift);
        unusedBook.setGate(gate);
        unusedBook.setDeviceId("DEV-303");
        unusedBook.setStartNum(1000);
        unusedBook.setNextTicket(1000);
        unusedBook.setStatus("Returned");

        when(ticketBookRepository.findByShift_ShiftId(shiftId)).thenReturn(List.of(cashBook, creditBook, unusedBook));

        // Credit Report for Credit Device (DEV-202)
        CreditReport creditReport = new CreditReport();
        creditReport.setCreditReportId(UUID.randomUUID());
        creditReport.setShift(shift);
        creditReport.setGate(gate);
        creditReport.setDeviceId("DEV-202");
        creditReport.setTotalAmount(BigDecimal.valueOf(658.00)); // 40 tix * $16.45 = $658.00

        when(creditReportRepository.findByShift_ShiftId(shiftId)).thenReturn(List.of(creditReport));

        ShiftReportDto result = reportService.getShiftReport(shiftId);

        assertThat(result).isNotNull();
        assertThat(result.getShiftId()).isEqualTo(shiftId);
        assertThat(result.getReportData()).hasSize(1);

        GateReportDto gateReport = result.getReportData().get(0);
        assertThat(gateReport.getDevices()).hasSize(2);

        // DEV-101 (Cash)
        DeviceReportDto dev101 = gateReport.getDevices().stream()
            .filter(d -> d.getDeviceId().equals("DEV-101"))
            .findFirst()
            .orElseThrow();
        assertThat(dev101.isCredit()).isFalse();
        assertThat(dev101.getBankAmount()).isEqualByComparingTo(BigDecimal.valueOf(700.00)); // 500 + 200
        assertThat(dev101.getTicketsSold()).isEqualTo(50);
        assertThat(dev101.getTicketValue()).isEqualByComparingTo(BigDecimal.valueOf(750.00)); // 50 * 15
        assertThat(dev101.getTicketBankValue()).isEqualByComparingTo(BigDecimal.valueOf(1450.00)); // 750 + 700
        assertThat(dev101.getCashCcAmount()).isEqualByComparingTo(BigDecimal.valueOf(1250.00)); // Deduplicated official count
        assertThat(dev101.getDifference()).isEqualByComparingTo(BigDecimal.valueOf(-200.00)); // 1250 - 1450 = -200

        // DEV-202 (Credit)
        DeviceReportDto dev202 = gateReport.getDevices().stream()
            .filter(d -> d.getDeviceId().equals("DEV-202"))
            .findFirst()
            .orElseThrow();
        assertThat(dev202.isCredit()).isTrue();
        assertThat(dev202.getBankAmount()).isEqualByComparingTo(BigDecimal.ZERO); // Strictly $0 for credit
        assertThat(dev202.getTicketsSold()).isEqualTo(40);
        assertThat(dev202.getTicketValue()).isEqualByComparingTo(BigDecimal.valueOf(658.00)); // 40 * (15 + 1.45) = 658
        assertThat(dev202.getCashCcAmount()).isEqualByComparingTo(BigDecimal.valueOf(658.00));
        assertThat(dev202.getDifference()).isEqualByComparingTo(BigDecimal.ZERO);

        // Bank Summary
        assertThat(result.getBankSummary()).hasSize(1);
        GateBankSummaryDto bankSum = result.getBankSummary().get(0);
        assertThat(bankSum.getIssued()).isEqualTo(3);
        assertThat(bankSum.getUsed()).isEqualTo(2);
        assertThat(bankSum.getNotUsed()).isEqualTo(1);

        // Sales Summary
        assertThat(result.getSalesSummary()).hasSize(1);
        GateSalesSummaryDto salesSum = result.getSalesSummary().get(0);
        assertThat(salesSum.getCashQty()).isEqualTo(50);
        assertThat(salesSum.getCashValue()).isEqualByComparingTo(BigDecimal.valueOf(750.00));
        assertThat(salesSum.getCcQty()).isEqualTo(40);
        assertThat(salesSum.getCcValue()).isEqualByComparingTo(BigDecimal.valueOf(658.00));
        assertThat(salesSum.getTotalQty()).isEqualTo(90);
        assertThat(salesSum.getTotalValue()).isEqualByComparingTo(BigDecimal.valueOf(1408.00));
    }

    @Test
    void getOperationReport_shouldAggregateAll14Sessions() {
        when(operationRepository.findById(operationId)).thenReturn(Optional.of(operation));
        when(gateRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc()).thenReturn(List.of(gate));
        when(shiftRepository.findByOperation_OperationId(operationId)).thenReturn(List.of(shift));
        when(cashBankRepository.findByOperation_OperationId(operationId)).thenReturn(List.of());
        when(ticketBookRepository.findByOperation_OperationId(operationId)).thenReturn(List.of());
        when(creditReportRepository.findByOperation_OperationId(operationId)).thenReturn(List.of());

        OperationReportDto result = reportService.getOperationReport(operationId);

        assertThat(result).isNotNull();
        assertThat(result.getOperationId()).isEqualTo(operationId);
        assertThat(result.getGates()).hasSize(1);
        assertThat(result.getSections()).containsKeys("combined", "cash", "credit");
        assertThat(result.getSections().get("combined")).hasSize(14); // All 14 sessions
        assertThat(result.getSectionTotals()).containsKeys("combined", "cash", "credit");
    }
}
