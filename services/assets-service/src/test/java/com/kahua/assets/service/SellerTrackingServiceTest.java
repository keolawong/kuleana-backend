package com.kahua.assets.service;

import com.kahua.assets.dto.SellerPerformanceDto;
import com.kahua.assets.entity.*;
import com.kahua.assets.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SellerTrackingServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private CashBankRepository cashBankRepository;

    @Mock
    private CashBankHistoryRepository cashBankHistoryRepository;

    @Mock
    private TicketBookRepository ticketBookRepository;

    @Mock
    private TicketBookHistoryRepository ticketBookHistoryRepository;

    @Mock
    private CreditReportRepository creditReportRepository;

    @InjectMocks
    private SellerTrackingService sellerTrackingService;

    private Person person1;
    private Person person2;
    private Shift shift1;
    private Operation operation1;
    private Gate gate1;

    @BeforeEach
    void setUp() {
        operation1 = new Operation();
        operation1.setOperationId(UUID.randomUUID());
        operation1.setName("Test Fiesta");
        operation1.setTicketPrice(new BigDecimal("15.00"));
        operation1.setCcFee(new BigDecimal("1.00"));

        shift1 = new Shift();
        shift1.setShiftId(UUID.randomUUID());
        shift1.setName("Shift 1 - Morning");
        shift1.setOperation(operation1);
        shift1.setEndTime(Instant.now());

        gate1 = new Gate();
        gate1.setGateId(UUID.randomUUID());
        gate1.setName("North Gate");

        person1 = new Person();
        person1.setPersonId(UUID.randomUUID());
        person1.setExternalId("VOL-100");
        person1.setFirstName("Alice");
        person1.setLastName("Smith");
        person1.setOrganization("Youth Group A");
        person1.setShift(shift1);
        person1.setGate(gate1);

        person2 = new Person();
        person2.setPersonId(UUID.randomUUID());
        person2.setExternalId(null); // tests null externalId fallback
        person2.setFirstName("Bob");
        person2.setLastName("Jones");
        person2.setOrganization(null); // tests null organization fallback
    }

    @Test
    @DisplayName("Should return empty list when no persons exist")
    void getSellerPerformance_emptyPersons() {
        when(personRepository.findAll()).thenReturn(Collections.emptyList());

        List<SellerPerformanceDto> result = sellerTrackingService.getSellerPerformance(null, null, null, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should gracefully handle records with null associations, null fields, and missing history")
    void getSellerPerformance_nullSafetyAndOrphanedRecords() {
        // Person with null fields
        Person corruptPerson = new Person();
        corruptPerson.setPersonId(UUID.randomUUID());
        corruptPerson.setExternalId(null);
        corruptPerson.setFirstName(null);
        corruptPerson.setLastName(null);

        when(personRepository.findAll()).thenReturn(List.of(person1, corruptPerson));

        // CashBank with null shift, null gate, null person
        CashBank bankWithNulls = new CashBank();
        bankWithNulls.setCashBankId(UUID.randomUUID());
        bankWithNulls.setPerson(null);
        bankWithNulls.setShift(null);
        bankWithNulls.setGate(null);

        // Valid cash bank for person1
        CashBank validBank = new CashBank();
        validBank.setCashBankId(UUID.randomUUID());
        validBank.setPerson(person1);
        validBank.setShift(shift1);
        validBank.setGate(gate1);
        validBank.setStartingAmount(new BigDecimal("100.00"));
        validBank.setOfficialCount(new BigDecimal("400.00"));
        validBank.setStatus("RETURNED");

        when(cashBankRepository.findAll()).thenReturn(List.of(bankWithNulls, validBank));

        // TicketBook with nextTicket < startNum or null values
        TicketBook tbWithNulls = new TicketBook();
        tbWithNulls.setTicketBookId(UUID.randomUUID());
        tbWithNulls.setPerson(person1);
        tbWithNulls.setShift(shift1);
        tbWithNulls.setGate(gate1);
        tbWithNulls.setStartNum(100);
        tbWithNulls.setNextTicket(50); // next < start!

        TicketBook validTb = new TicketBook();
        validTb.setTicketBookId(UUID.randomUUID());
        validTb.setPerson(person1);
        validTb.setShift(shift1);
        validTb.setGate(gate1);
        validTb.setStartNum(1);
        validTb.setNextTicket(21); // sold 20 tickets * $15 = $300 expected

        when(ticketBookRepository.findAll()).thenReturn(List.of(tbWithNulls, validTb));

        // History records with orphaned parents
        CashBankHistory cbHistory = new CashBankHistory();
        cbHistory.setCashBankHistoryId(UUID.randomUUID());
        cbHistory.setCashBankId(UUID.randomUUID()); // unlinked parent
        cbHistory.setShift(shift1);
        when(cashBankHistoryRepository.findAll()).thenReturn(List.of(cbHistory));

        TicketBookHistory tbHistory = new TicketBookHistory();
        tbHistory.setTicketBookHistoryId(UUID.randomUUID());
        tbHistory.setTicketBookId(UUID.randomUUID()); // unlinked parent
        tbHistory.setShift(shift1);
        when(ticketBookHistoryRepository.findAll()).thenReturn(List.of(tbHistory));

        when(creditReportRepository.findAll()).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> {
            List<SellerPerformanceDto> results = sellerTrackingService.getSellerPerformance(null, null, null, null);
            assertThat(results).isNotEmpty();
            SellerPerformanceDto alice = results.stream()
                    .filter(p -> "VOL-100".equals(p.getExternalId()))
                    .findFirst()
                    .orElse(null);

            assertThat(alice).isNotNull();
            assertThat(alice.getSellerName()).isEqualTo("Alice Smith");
            assertThat(alice.getTotalSessions()).isEqualTo(1);
            // Expected: 20 tickets * 15 = 300 + 100 starting = 400. Collected = 400. Variance = 0.
            assertThat(alice.getTotalExpected()).isEqualByComparingTo(new BigDecimal("400.00"));
            assertThat(alice.getTotalCollected()).isEqualByComparingTo(new BigDecimal("400.00"));
            assertThat(alice.getNetVariance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(alice.getPerformanceStatus()).isEqualTo("BALANCED");
        });
    }

    @Test
    @DisplayName("Should correctly distinguish Credit Card sessions from Cash sessions")
    void getSellerPerformance_creditSessionHandling() {
        when(personRepository.findAll()).thenReturn(List.of(person1));
        when(cashBankRepository.findAll()).thenReturn(Collections.emptyList());
        when(cashBankHistoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(ticketBookHistoryRepository.findAll()).thenReturn(Collections.emptyList());

        // Person has ticket book and credit report only -> CREDIT session
        TicketBook creditTb = new TicketBook();
        creditTb.setTicketBookId(UUID.randomUUID());
        creditTb.setPerson(person1);
        creditTb.setShift(shift1);
        creditTb.setGate(gate1);
        creditTb.setStartNum(1);
        creditTb.setNextTicket(11); // 10 tickets sold

        when(ticketBookRepository.findAll()).thenReturn(List.of(creditTb));

        CreditReport cr = new CreditReport();
        cr.setCreditReportId(UUID.randomUUID());
        cr.setPerson(person1);
        cr.setShift(shift1);
        cr.setGate(gate1);
        cr.setTotalAmount(new BigDecimal("160.00")); // 10 tickets * (15 + 1 ccFee) = 160 expected

        when(creditReportRepository.findAll()).thenReturn(List.of(cr));

        List<SellerPerformanceDto> results = sellerTrackingService.getSellerPerformance(null, null, null, null);
        assertThat(results).hasSize(1);
        SellerPerformanceDto seller = results.get(0);
        assertThat(seller.getSessions()).hasSize(1);
        assertThat(seller.getSessions().get(0).getSellerType()).isEqualTo("CREDIT");
        assertThat(seller.getSessions().get(0).getExpectedAmount()).isEqualByComparingTo(new BigDecimal("160.00"));
        assertThat(seller.getSessions().get(0).getCollectedAmount()).isEqualByComparingTo(new BigDecimal("160.00"));
        assertThat(seller.getPerformanceStatus()).isEqualTo("BALANCED");
    }

    @Test
    @DisplayName("Should exclude sessions from inactive operations when activeOperationsOnly is true")
    void getSellerPerformance_inactiveOperationFilter() {
        operation1.setIsActive(false); // Inactive operation

        when(personRepository.findAll()).thenReturn(List.of(person1));
        when(cashBankRepository.findAll()).thenReturn(Collections.emptyList());
        when(cashBankHistoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(ticketBookHistoryRepository.findAll()).thenReturn(Collections.emptyList());

        TicketBook tb = new TicketBook();
        tb.setTicketBookId(UUID.randomUUID());
        tb.setPerson(person1);
        tb.setShift(shift1);
        tb.setGate(gate1);
        tb.setStartNum(1);
        tb.setNextTicket(11);

        when(ticketBookRepository.findAll()).thenReturn(List.of(tb));

        CreditReport cr = new CreditReport();
        cr.setCreditReportId(UUID.randomUUID());
        cr.setPerson(person1);
        cr.setShift(shift1);
        cr.setGate(gate1);
        cr.setTotalAmount(new BigDecimal("160.00"));

        when(creditReportRepository.findAll()).thenReturn(List.of(cr));

        // When activeOperationsOnly is true (default), inactive operation sessions are filtered out
        List<SellerPerformanceDto> activeResults = sellerTrackingService.getSellerPerformance(null, null, null, null, true);
        assertThat(activeResults).isEmpty();

        // When activeOperationsOnly is explicitly false, inactive operation sessions are included
        List<SellerPerformanceDto> allResults = sellerTrackingService.getSellerPerformance(null, null, null, null, false);
        assertThat(allResults).hasSize(1);
    }

    @Test
    @DisplayName("Should calculate accuracy rate as 100% when seller is over expected amount")
    void getSellerPerformance_overAccuracyRate() {
        operation1.setIsActive(true);

        when(personRepository.findAll()).thenReturn(List.of(person1));
        when(cashBankRepository.findAll()).thenReturn(Collections.emptyList());
        when(cashBankHistoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(ticketBookHistoryRepository.findAll()).thenReturn(Collections.emptyList());

        TicketBook tb = new TicketBook();
        tb.setTicketBookId(UUID.randomUUID());
        tb.setPerson(person1);
        tb.setShift(shift1);
        tb.setGate(gate1);
        tb.setStartNum(1);
        tb.setNextTicket(11); // 10 tickets sold * $16 = $160 expected

        when(ticketBookRepository.findAll()).thenReturn(List.of(tb));

        CreditReport cr = new CreditReport();
        cr.setCreditReportId(UUID.randomUUID());
        cr.setPerson(person1);
        cr.setShift(shift1);
        cr.setGate(gate1);
        cr.setTotalAmount(new BigDecimal("180.00")); // $180 collected ($20 OVER)

        when(creditReportRepository.findAll()).thenReturn(List.of(cr));

        List<SellerPerformanceDto> results = sellerTrackingService.getSellerPerformance(null, null, null, null);
        assertThat(results).hasSize(1);
        SellerPerformanceDto seller = results.get(0);
        assertThat(seller.getNetVariance()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(seller.getPerformanceStatus()).isEqualTo("OVER");
        // Accuracy should be 100.00% when over, not penalized
        assertThat(seller.getAccuracyRate()).isEqualByComparingTo(new BigDecimal("100.00"));
    }
}
