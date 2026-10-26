package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.dto.SellerPerformanceDto;
import com.kuleanasystems.assets.dto.SellerSessionStubDto;
import com.kuleanasystems.assets.entity.*;
import com.kuleanasystems.assets.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerTrackingService {

    private final PersonRepository personRepository;
    private final CashBankRepository cashBankRepository;
    private final CashBankHistoryRepository cashBankHistoryRepository;
    private final TicketBookRepository ticketBookRepository;
    private final TicketBookHistoryRepository ticketBookHistoryRepository;
    private final CreditReportRepository creditReportRepository;

    @Transactional(readOnly = true)
    public List<SellerPerformanceDto> getSellerPerformance(UUID operationId, UUID shiftId, UUID gateId, String search) {
        return getSellerPerformance(operationId, shiftId, gateId, search, true);
    }

    @Transactional(readOnly = true)
    public List<SellerPerformanceDto> getSellerPerformance(UUID operationId, UUID shiftId, UUID gateId, String search, Boolean activeOperationsOnly) {
        log.info("Computing seller performance with filters: operationId={}, shiftId={}, gateId={}, search={}, activeOperationsOnly={}",
                operationId, shiftId, gateId, search, activeOperationsOnly);

        // 1. Fetch all persons
        List<Person> allPersons = personRepository.findAll();
        if (allPersons.isEmpty()) {
            return Collections.emptyList();
        }

        // Filter persons if search provided
        String searchLower = (search != null && !search.trim().isEmpty()) ? search.trim().toLowerCase() : null;

        // Group persons by externalId if present, else personId string
        // Each externalId belongs to a single physical person with a single organization
        Map<String, List<Person>> personsByExternalId = allPersons.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(p -> {
                    if (p.getExternalId() != null && !p.getExternalId().trim().isEmpty()) {
                        return p.getExternalId().trim();
                    }
                    return p.getPersonId() != null ? p.getPersonId().toString() : UUID.randomUUID().toString();
                }));

        // 2. Fetch all CashBanks, TicketBooks, and CreditReports
        // We fetch active and historical records
        List<CashBank> cashBanks = cashBankRepository.findAll();
        List<CashBankHistory> cashBankHistories = cashBankHistoryRepository.findAll();
        List<TicketBook> ticketBooks = ticketBookRepository.findAll();
        List<TicketBookHistory> ticketBookHistories = ticketBookHistoryRepository.findAll();
        List<CreditReport> creditReports = creditReportRepository.findAll();

        // 3. For each group of persons (same externalId), compute session stubs
        List<SellerPerformanceDto> performanceList = new ArrayList<>();

        for (Map.Entry<String, List<Person>> entry : personsByExternalId.entrySet()) {
            try {
                String groupKey = entry.getKey();
                List<Person> personInstances = entry.getValue();
                if (personInstances == null || personInstances.isEmpty()) {
                    continue;
                }

                // Extract primary person identity info
                Person primaryPerson = personInstances.get(0);
                String externalId = (primaryPerson.getExternalId() != null && !primaryPerson.getExternalId().trim().isEmpty())
                        ? primaryPerson.getExternalId().trim()
                        : groupKey;
                String firstName = primaryPerson.getFirstName() != null ? primaryPerson.getFirstName().trim() : "";
                String lastName = primaryPerson.getLastName() != null ? primaryPerson.getLastName().trim() : "";
                String sellerName = (firstName + " " + lastName).trim();
                if (sellerName.isEmpty()) {
                    sellerName = "Unknown Seller (" + externalId + ")";
                }
                String organization = primaryPerson.getOrganization() != null ? primaryPerson.getOrganization() : "Individual / None";

                // If search filter provided, filter on externalId, sellerName, or organization
                if (searchLower != null) {
                    boolean matches = externalId.toLowerCase().contains(searchLower)
                            || sellerName.toLowerCase().contains(searchLower)
                            || organization.toLowerCase().contains(searchLower);
                    if (!matches) {
                        continue;
                    }
                }

                Set<UUID> allPersonIdsInGroup = personInstances.stream()
                        .map(Person::getPersonId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                // Collect all unique shifts where this person had assigned assets or credit reports
                Set<UUID> workedShiftIds = new HashSet<>();

                // Cash banks assigned to this person group
                List<CashBank> personBanks = cashBanks.stream()
                        .filter(Objects::nonNull)
                        .filter(b -> b.getPerson() != null && b.getPerson().getPersonId() != null && allPersonIdsInGroup.contains(b.getPerson().getPersonId()))
                        .filter(b -> b.getShift() != null && b.getShift().getShiftId() != null)
                        .collect(Collectors.toList());

                // Ticket books assigned to this person group
                List<TicketBook> personBooks = ticketBooks.stream()
                        .filter(Objects::nonNull)
                        .filter(b -> b.getPerson() != null && b.getPerson().getPersonId() != null && allPersonIdsInGroup.contains(b.getPerson().getPersonId()))
                        .filter(b -> b.getShift() != null && b.getShift().getShiftId() != null)
                        .collect(Collectors.toList());

                // Credit reports assigned to this person group
                List<CreditReport> personCredits = creditReports.stream()
                        .filter(Objects::nonNull)
                        .filter(c -> c.getPerson() != null && c.getPerson().getPersonId() != null && allPersonIdsInGroup.contains(c.getPerson().getPersonId()))
                        .filter(c -> c.getShift() != null && c.getShift().getShiftId() != null)
                        .collect(Collectors.toList());

                personBanks.forEach(b -> workedShiftIds.add(b.getShift().getShiftId()));
                personBooks.forEach(b -> workedShiftIds.add(b.getShift().getShiftId()));
                personCredits.forEach(c -> workedShiftIds.add(c.getShift().getShiftId()));

                // Also check history for shifts that were reconciled/closed
                List<CashBankHistory> personBankHistories = cashBankHistories.stream()
                        .filter(Objects::nonNull)
                        .filter(b -> b.getShift() != null && b.getShift().getShiftId() != null && b.getCashBankId() != null)
                        .filter(b -> {
                            CashBank active = cashBanks.stream()
                                    .filter(Objects::nonNull)
                                    .filter(cb -> b.getCashBankId().equals(cb.getCashBankId()))
                                    .findFirst()
                                    .orElse(null);
                            return active != null && active.getPerson() != null && active.getPerson().getPersonId() != null
                                    && allPersonIdsInGroup.contains(active.getPerson().getPersonId());
                        })
                        .collect(Collectors.toList());
                personBankHistories.forEach(b -> workedShiftIds.add(b.getShift().getShiftId()));

                List<TicketBookHistory> personBookHistories = ticketBookHistories.stream()
                        .filter(Objects::nonNull)
                        .filter(b -> b.getShift() != null && b.getShift().getShiftId() != null && b.getTicketBookId() != null)
                        .filter(b -> {
                            TicketBook active = ticketBooks.stream()
                                    .filter(Objects::nonNull)
                                    .filter(tb -> b.getTicketBookId().equals(tb.getTicketBookId()))
                                    .findFirst()
                                    .orElse(null);
                            return active != null && active.getPerson() != null && active.getPerson().getPersonId() != null
                                    && allPersonIdsInGroup.contains(active.getPerson().getPersonId());
                        })
                        .collect(Collectors.toList());
                personBookHistories.forEach(b -> workedShiftIds.add(b.getShift().getShiftId()));

                // Now compute session stubs for each shift
                List<SellerSessionStubDto> sessionStubs = new ArrayList<>();

                for (UUID sId : workedShiftIds) {
                    if (sId == null) {
                        continue;
                    }

                    // If filtering by shiftId
                    if (shiftId != null && !sId.equals(shiftId)) {
                        continue;
                    }

                    // Filter items for this shift
                    List<CashBank> shiftBanks = personBanks.stream()
                            .filter(b -> b.getShift() != null && sId.equals(b.getShift().getShiftId()))
                            .collect(Collectors.toList());
                    List<TicketBook> shiftBooks = personBooks.stream()
                            .filter(b -> b.getShift() != null && sId.equals(b.getShift().getShiftId()))
                            .collect(Collectors.toList());
                    List<CreditReport> shiftCredits = personCredits.stream()
                            .filter(c -> c.getShift() != null && sId.equals(c.getShift().getShiftId()))
                            .collect(Collectors.toList());

                    // Find the associated shift & operation & gate
                    Shift shiftEntity = null;
                    Gate gateEntity = null;
                    String deviceId = null;

                    if (!shiftBanks.isEmpty()) {
                        shiftEntity = shiftBanks.get(0).getShift();
                        gateEntity = shiftBanks.get(0).getGate();
                        deviceId = shiftBanks.get(0).getDeviceId();
                    } else if (!shiftBooks.isEmpty()) {
                        shiftEntity = shiftBooks.get(0).getShift();
                        gateEntity = shiftBooks.get(0).getGate();
                        deviceId = shiftBooks.get(0).getDeviceId();
                    } else if (!shiftCredits.isEmpty()) {
                        shiftEntity = shiftCredits.get(0).getShift();
                        gateEntity = shiftCredits.get(0).getGate();
                        deviceId = shiftCredits.get(0).getDeviceId();
                    }

                    if (shiftEntity == null) {
                        continue;
                    }

                    // Operation filter
                    if (operationId != null && (shiftEntity.getOperation() == null
                            || shiftEntity.getOperation().getOperationId() == null
                            || !shiftEntity.getOperation().getOperationId().equals(operationId))) {
                        continue;
                    }

                    // Only show seller sessions from active events/operations
                    if (Boolean.TRUE.equals(activeOperationsOnly)) {
                        if (shiftEntity.getOperation() == null
                                || !Boolean.TRUE.equals(shiftEntity.getOperation().getIsActive())) {
                            continue;
                        }
                    }

                    // Gate filter
                    if (gateId != null && (gateEntity == null
                            || gateEntity.getGateId() == null
                            || !gateEntity.getGateId().equals(gateId))) {
                        continue;
                    }

                    Operation opEntity = shiftEntity.getOperation();
                    BigDecimal ticketPrice = (opEntity != null && opEntity.getTicketPrice() != null) ? opEntity.getTicketPrice() : BigDecimal.ZERO;
                    BigDecimal ccFee = (opEntity != null && opEntity.getCcFee() != null) ? opEntity.getCcFee() : BigDecimal.ZERO;

                    // Tickets sold: nextTicket - startNum
                    int ticketsSold = 0;
                    for (TicketBook tb : shiftBooks) {
                        if (tb.getNextTicket() != null && tb.getStartNum() != null && tb.getNextTicket() >= tb.getStartNum()) {
                            ticketsSold += (tb.getNextTicket() - tb.getStartNum());
                        }
                    }

                    // Determine if this session is Cash or Credit
                    boolean isCredit = !shiftCredits.isEmpty() && shiftBanks.isEmpty();
                    String sellerType = isCredit ? "CREDIT" : "CASH";

                    BigDecimal startingAmount = BigDecimal.ZERO;
                    BigDecimal verifiedCash = BigDecimal.ZERO;
                    for (CashBank cb : shiftBanks) {
                        if (!"Returned Unused".equalsIgnoreCase(cb.getStatus())) {
                            if (cb.getStartingAmount() != null) {
                                startingAmount = startingAmount.add(cb.getStartingAmount());
                            }
                            if (cb.getOfficialCount() != null) {
                                verifiedCash = verifiedCash.add(cb.getOfficialCount());
                            }
                        }
                    }

                    BigDecimal cardReportTotal = BigDecimal.ZERO;
                    for (CreditReport cr : shiftCredits) {
                        if (cr.getTotalAmount() != null) {
                            cardReportTotal = cardReportTotal.add(cr.getTotalAmount());
                        }
                    }

                    // Expected ticket value
                    BigDecimal expectedTicketCash;
                    if (isCredit) {
                        expectedTicketCash = BigDecimal.valueOf(ticketsSold).multiply(ticketPrice.add(ccFee));
                    } else {
                        expectedTicketCash = BigDecimal.valueOf(ticketsSold).multiply(ticketPrice);
                    }

                    // Total collected: verified cash for Cash sessions, card report total for Credit sessions
                    BigDecimal collectedAmount = isCredit ? cardReportTotal : verifiedCash;

                    // Expected total: For cash, expectedTicketCash + startingAmount. For credit, expectedTicketCash
                    BigDecimal expectedTotal = isCredit ? expectedTicketCash : expectedTicketCash.add(startingAmount);

                    // Variance: collectedAmount - expectedTotal
                    BigDecimal sessionVariance = collectedAmount.subtract(expectedTotal);

                    SellerSessionStubDto stub = SellerSessionStubDto.builder()
                            .shiftId(shiftEntity.getShiftId())
                            .shiftName(shiftEntity.getName() != null ? shiftEntity.getName() : "Unnamed Session")
                            .operationName(opEntity != null && opEntity.getName() != null ? opEntity.getName() : "General Operation")
                            .gateId(gateEntity != null ? gateEntity.getGateId() : null)
                            .gateName(gateEntity != null && gateEntity.getName() != null ? gateEntity.getName() : "Unassigned Gate")
                            .deviceId(deviceId)
                            .sellerType(sellerType)
                            .ticketsSold(ticketsSold)
                            .startingAmount(startingAmount)
                            .expectedAmount(expectedTotal)
                            .collectedAmount(collectedAmount)
                            .variance(sessionVariance)
                            .reconciledAt(shiftEntity.getEndTime() != null ? shiftEntity.getEndTime().toString() : null)
                            .build();

                    sessionStubs.add(stub);
                }

                if (sessionStubs.isEmpty()) {
                    continue;
                }

                // Sort session stubs chronologically by shift name
                sessionStubs.sort(Comparator.comparing(
                        s -> s.getShiftName() != null ? s.getShiftName() : "",
                        String::compareToIgnoreCase
                ));

                // Compute running performance metrics for seller
                int totalSessions = sessionStubs.size();
                int balancedSessions = 0;
                int overSessions = 0;
                int underSessions = 0;

                BigDecimal netVariance = BigDecimal.ZERO;
                BigDecimal totalAbsoluteDiscrepancy = BigDecimal.ZERO;
                BigDecimal totalExpected = BigDecimal.ZERO;
                BigDecimal totalCollected = BigDecimal.ZERO;

                for (SellerSessionStubDto stub : sessionStubs) {
                    BigDecimal exp = stub.getExpectedAmount() != null ? stub.getExpectedAmount() : BigDecimal.ZERO;
                    BigDecimal col = stub.getCollectedAmount() != null ? stub.getCollectedAmount() : BigDecimal.ZERO;
                    BigDecimal var = stub.getVariance() != null ? stub.getVariance() : BigDecimal.ZERO;

                    totalExpected = totalExpected.add(exp);
                    totalCollected = totalCollected.add(col);

                    netVariance = netVariance.add(var);
                    totalAbsoluteDiscrepancy = totalAbsoluteDiscrepancy.add(var.abs());

                    int cmp = var.compareTo(BigDecimal.ZERO);
                    if (cmp == 0) {
                        balancedSessions++;
                    } else if (cmp > 0) {
                        overSessions++;
                    } else {
                        underSessions++;
                    }
                }

                BigDecimal accuracyRate;
                if (totalExpected.compareTo(BigDecimal.ZERO) > 0) {
                    if (totalCollected.compareTo(totalExpected) >= 0) {
                        accuracyRate = BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP);
                    } else {
                        BigDecimal ratio = totalCollected.divide(totalExpected, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
                        accuracyRate = ratio.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100.00)).setScale(2, RoundingMode.HALF_UP);
                    }
                } else {
                    accuracyRate = BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP);
                }

                String performanceStatus;
                int netCmp = netVariance.compareTo(BigDecimal.ZERO);
                if (netCmp > 0) {
                    performanceStatus = "OVER";
                } else if (netCmp < 0) {
                    performanceStatus = "UNDER";
                } else {
                    performanceStatus = "BALANCED";
                }

                SellerPerformanceDto dto = SellerPerformanceDto.builder()
                        .externalId(externalId)
                        .sellerName(sellerName)
                        .personId(primaryPerson.getPersonId())
                        .organization(organization)
                        .totalSessions(totalSessions)
                        .balancedSessions(balancedSessions)
                        .overSessions(overSessions)
                        .underSessions(underSessions)
                        .netVariance(netVariance)
                        .totalAbsoluteDiscrepancy(totalAbsoluteDiscrepancy)
                        .totalExpected(totalExpected)
                        .totalCollected(totalCollected)
                        .accuracyRate(accuracyRate)
                        .performanceStatus(performanceStatus)
                        .sessions(sessionStubs)
                        .build();

                performanceList.add(dto);
            } catch (Exception ex) {
                log.error("Error processing seller performance for entry: key={}", entry.getKey(), ex);
            }
        }

        // Sort performanceList alphabetically by externalId
        performanceList.sort(Comparator.comparing(
                p -> p.getExternalId() != null ? p.getExternalId() : "",
                String::compareToIgnoreCase
        ));

        log.info("Successfully calculated performance for {} sellers", performanceList.size());
        return performanceList;
    }
}
