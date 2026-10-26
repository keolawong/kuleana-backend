package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.dto.report.*;
import com.kuleanasystems.assets.entity.*;
import com.kuleanasystems.assets.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ShiftRepository shiftRepository;
    private final OperationRepository operationRepository;
    private final GateRepository gateRepository;
    private final CashBankRepository cashBankRepository;
    private final TicketBookRepository ticketBookRepository;
    private final CreditReportRepository creditReportRepository;

    public record SessionConfig(int sessionNum, String day, String time) {}

    public static final List<SessionConfig> SESSIONS_CONFIG = List.of(
        new SessionConfig(1, "Saturday", "AM"),
        new SessionConfig(2, "Saturday", "PM"),
        new SessionConfig(3, "Sunday", "AM"),
        new SessionConfig(4, "Sunday", "PM"),
        new SessionConfig(5, "Monday", "AM"),
        new SessionConfig(6, "Tuesday", "AM"),
        new SessionConfig(7, "Wednesday", "AM"),
        new SessionConfig(8, "Thursday", "AM"),
        new SessionConfig(9, "Thursday", "PM"),
        new SessionConfig(10, "Friday", "AM"),
        new SessionConfig(11, "Friday", "PM"),
        new SessionConfig(12, "Saturday", "AM"),
        new SessionConfig(13, "Saturday", "PM"),
        new SessionConfig(14, "Sunday", "AM")
    );

    public ShiftReportDto getShiftReport(UUID shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found: " + shiftId));

        Operation operation = shift.getOperation();
        BigDecimal ticketPrice = (operation != null && operation.getTicketPrice() != null)
            ? operation.getTicketPrice() : BigDecimal.valueOf(15.00);
        BigDecimal ccFee = (operation != null && operation.getCcFee() != null)
            ? operation.getCcFee() : BigDecimal.valueOf(1.45);

        List<Gate> allGates = gateRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc();
        List<CashBank> rawBanks = cashBankRepository.findByShift_ShiftId(shiftId);
        List<TicketBook> rawBooks = ticketBookRepository.findByShift_ShiftId(shiftId);
        List<CreditReport> rawCredits = creditReportRepository.findByShift_ShiftId(shiftId);

        List<GateReportDto> reportData = new ArrayList<>();
        List<GateBankSummaryDto> bankSummary = new ArrayList<>();
        List<GateSalesSummaryDto> salesSummary = new ArrayList<>();

        BigDecimal totalShiftBankAmount = BigDecimal.ZERO;
        int totalShiftTicketsSold = 0;
        BigDecimal totalShiftTicketValue = BigDecimal.ZERO;
        BigDecimal totalShiftTicketBankValue = BigDecimal.ZERO;
        BigDecimal totalShiftCashCcAmount = BigDecimal.ZERO;
        BigDecimal totalShiftDifference = BigDecimal.ZERO;

        for (Gate gate : allGates) {
            UUID gId = gate.getGateId();
            List<CashBank> gateBanks = rawBanks.stream()
                .filter(b -> b.getGate() != null && gId.equals(b.getGate().getGateId()))
                .collect(Collectors.toList());
            List<TicketBook> gateBooks = rawBooks.stream()
                .filter(b -> b.getGate() != null && gId.equals(b.getGate().getGateId()))
                .collect(Collectors.toList());
            List<CreditReport> gateCredits = rawCredits.stream()
                .filter(c -> c.getGate() != null && gId.equals(c.getGate().getGateId()))
                .collect(Collectors.toList());

            GateFinancials gf = computeGateFinancials(gate, gateBanks, gateBooks, gateCredits, ticketPrice, ccFee);

            if (!gf.devices().isEmpty()) {
                reportData.add(GateReportDto.builder()
                    .gateId(gId)
                    .gateName(gate.getName())
                    .displayOrder(gate.getDisplayOrder() != null ? gate.getDisplayOrder() : 9999)
                    .devices(gf.devices())
                    .totals(gf.totals())
                    .build());
            }

            if (gf.bankSummary().getIssued() > 0) {
                bankSummary.add(gf.bankSummary());
            }

            if (!gf.devices().isEmpty() || gf.salesSummary().getTotalQty() > 0) {
                salesSummary.add(gf.salesSummary());
            }

            totalShiftBankAmount = totalShiftBankAmount.add(gf.totals().getBankAmount());
            totalShiftTicketsSold += gf.totals().getTicketsSold();
            totalShiftTicketValue = totalShiftTicketValue.add(gf.totals().getTicketValue());
            totalShiftTicketBankValue = totalShiftTicketBankValue.add(gf.totals().getTicketBankValue());
            totalShiftCashCcAmount = totalShiftCashCcAmount.add(gf.totals().getCashCcAmount());
            totalShiftDifference = totalShiftDifference.add(gf.totals().getDifference());
        }

        GateTotalsDto shiftTotals = GateTotalsDto.builder()
            .bankAmount(totalShiftBankAmount)
            .ticketsSold(totalShiftTicketsSold)
            .ticketValue(totalShiftTicketValue)
            .ticketBankValue(totalShiftTicketBankValue)
            .cashCcAmount(totalShiftCashCcAmount)
            .difference(totalShiftDifference)
            .build();

        return ShiftReportDto.builder()
            .shiftId(shiftId)
            .shiftName(shift.getName())
            .startTime(shift.getStartTime())
            .endTime(shift.getEndTime())
            .operationId(operation != null ? operation.getOperationId() : null)
            .operationName(operation != null ? operation.getName() : null)
            .ticketPrice(ticketPrice)
            .ccFee(ccFee)
            .reportData(reportData)
            .bankSummary(bankSummary)
            .salesSummary(salesSummary)
            .totals(shiftTotals)
            .build();
    }

    public OperationReportDto getOperationReport(UUID operationId) {
        Operation operation = operationRepository.findById(operationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operation not found: " + operationId));

        BigDecimal ticketPrice = operation.getTicketPrice() != null
            ? operation.getTicketPrice() : BigDecimal.valueOf(15.00);
        BigDecimal ccFee = operation.getCcFee() != null
            ? operation.getCcFee() : BigDecimal.valueOf(1.45);

        List<Gate> allGates = gateRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc();
        List<GateHeaderDto> gateHeaders = allGates.stream()
            .map(g -> GateHeaderDto.builder()
                .gateId(g.getGateId())
                .name(g.getName())
                .displayOrder(g.getDisplayOrder() != null ? g.getDisplayOrder() : 9999)
                .build())
            .collect(Collectors.toList());

        List<Shift> shifts = shiftRepository.findByOperation_OperationId(operationId);
        List<CashBank> rawBanks = cashBankRepository.findByOperation_OperationId(operationId);
        List<TicketBook> rawBooks = ticketBookRepository.findByOperation_OperationId(operationId);
        List<CreditReport> rawCredits = creditReportRepository.findByOperation_OperationId(operationId);

        // Precompute gate financials per shift and gate
        // Key: shiftId -> (gateId -> GateFinancials)
        Map<UUID, Map<UUID, GateFinancials>> shiftGateFinancials = new HashMap<>();
        for (Shift s : shifts) {
            UUID sId = s.getShiftId();
            List<CashBank> sBanks = rawBanks.stream()
                .filter(b -> b.getShift() != null && sId.equals(b.getShift().getShiftId()))
                .collect(Collectors.toList());
            List<TicketBook> sBooks = rawBooks.stream()
                .filter(b -> b.getShift() != null && sId.equals(b.getShift().getShiftId()))
                .collect(Collectors.toList());
            List<CreditReport> sCredits = rawCredits.stream()
                .filter(c -> c.getShift() != null && sId.equals(c.getShift().getShiftId()))
                .collect(Collectors.toList());

            Map<UUID, GateFinancials> gateMap = new HashMap<>();
            for (Gate gate : allGates) {
                UUID gId = gate.getGateId();
                List<CashBank> gBanks = sBanks.stream()
                    .filter(b -> b.getGate() != null && gId.equals(b.getGate().getGateId()))
                    .collect(Collectors.toList());
                List<TicketBook> gBooks = sBooks.stream()
                    .filter(b -> b.getGate() != null && gId.equals(b.getGate().getGateId()))
                    .collect(Collectors.toList());
                List<CreditReport> gCredits = sCredits.stream()
                    .filter(c -> c.getGate() != null && gId.equals(c.getGate().getGateId()))
                    .collect(Collectors.toList());

                GateFinancials gf = computeGateFinancials(gate, gBanks, gBooks, gCredits, ticketPrice, ccFee);
                gateMap.put(gId, gf);
            }
            shiftGateFinancials.put(sId, gateMap);
        }

        List<OperationSectionRowDto> combinedRows = buildSectionRows("combined", shifts, allGates, shiftGateFinancials);
        List<OperationSectionRowDto> cashRows = buildSectionRows("cash", shifts, allGates, shiftGateFinancials);
        List<OperationSectionRowDto> creditRows = buildSectionRows("credit", shifts, allGates, shiftGateFinancials);

        Map<String, List<OperationSectionRowDto>> sections = Map.of(
            "combined", combinedRows,
            "cash", cashRows,
            "credit", creditRows
        );

        Map<String, Map<String, OperationTotalsDto>> sectionTotals = Map.of(
            "combined", calculateSectionTotals(combinedRows, allGates),
            "cash", calculateSectionTotals(cashRows, allGates),
            "credit", calculateSectionTotals(creditRows, allGates)
        );

        return OperationReportDto.builder()
            .operationId(operationId)
            .operationName(operation.getName())
            .ticketPrice(ticketPrice)
            .ccFee(ccFee)
            .gates(gateHeaders)
            .sections(sections)
            .sectionTotals(sectionTotals)
            .build();
    }

    private List<OperationSectionRowDto> buildSectionRows(
        String sectionType,
        List<Shift> shifts,
        List<Gate> gates,
        Map<UUID, Map<UUID, GateFinancials>> shiftGateFinancials
    ) {
        List<OperationSectionRowDto> rows = new ArrayList<>();

        for (SessionConfig cfg : SESSIONS_CONFIG) {
            Shift matchingShift = shifts.stream()
                .filter(s -> parseSessionNum(s.getName()) == cfg.sessionNum())
                .findFirst()
                .orElse(null);

            Map<String, OperationTotalsDto> rowData = new HashMap<>();
            int gtTickets = 0;
            BigDecimal gtMonetary = BigDecimal.ZERO;
            BigDecimal gtBanks = BigDecimal.ZERO;
            BigDecimal gtExpected = BigDecimal.ZERO;
            BigDecimal gtNet = BigDecimal.ZERO;
            BigDecimal gtOverUnder = BigDecimal.ZERO;

            for (Gate g : gates) {
                String gKey = g.getGateId().toString();
                OperationTotalsDto item;

                if (matchingShift != null && shiftGateFinancials.containsKey(matchingShift.getShiftId())) {
                    GateFinancials gf = shiftGateFinancials.get(matchingShift.getShiftId()).get(g.getGateId());
                    OperationTotalsDto sec = gf.operationSections().get(sectionType);
                    item = OperationTotalsDto.builder()
                        .sessionNum(cfg.sessionNum())
                        .day(cfg.day())
                        .time(cfg.time())
                        .tickets(sec.getTickets())
                        .monetaryAmt(sec.getMonetaryAmt())
                        .banks(sec.getBanks())
                        .expectedValue(sec.getExpectedValue())
                        .netSales(sec.getNetSales())
                        .overUnder(sec.getOverUnder())
                        .build();
                } else {
                    item = OperationTotalsDto.builder()
                        .sessionNum(cfg.sessionNum())
                        .day(cfg.day())
                        .time(cfg.time())
                        .tickets(0)
                        .monetaryAmt(BigDecimal.ZERO)
                        .banks(BigDecimal.ZERO)
                        .expectedValue(BigDecimal.ZERO)
                        .netSales(BigDecimal.ZERO)
                        .overUnder(BigDecimal.ZERO)
                        .build();
                }

                rowData.put(gKey, item);

                gtTickets += item.getTickets();
                gtMonetary = gtMonetary.add(item.getMonetaryAmt());
                gtBanks = gtBanks.add(item.getBanks());
                gtExpected = gtExpected.add(item.getExpectedValue());
                gtNet = gtNet.add(item.getNetSales());
                gtOverUnder = gtOverUnder.add(item.getOverUnder());
            }

            OperationTotalsDto grandTotals = OperationTotalsDto.builder()
                .sessionNum(cfg.sessionNum())
                .day(cfg.day())
                .time(cfg.time())
                .tickets(gtTickets)
                .monetaryAmt(gtMonetary)
                .banks(gtBanks)
                .expectedValue(gtExpected)
                .netSales(gtNet)
                .overUnder(gtOverUnder)
                .build();

            rowData.put("grandTotals", grandTotals);

            rows.add(OperationSectionRowDto.builder()
                .sessionNum(cfg.sessionNum())
                .day(cfg.day())
                .time(cfg.time())
                .data(rowData)
                .build());
        }

        return rows;
    }

    private Map<String, OperationTotalsDto> calculateSectionTotals(
        List<OperationSectionRowDto> rows,
        List<Gate> gates
    ) {
        Map<String, OperationTotalsDto> totals = new HashMap<>();

        List<String> keys = new ArrayList<>();
        keys.add("grandTotals");
        for (Gate g : gates) {
            keys.add(g.getGateId().toString());
        }

        for (String k : keys) {
            int tickets = 0;
            BigDecimal monetary = BigDecimal.ZERO;
            BigDecimal banks = BigDecimal.ZERO;
            BigDecimal expected = BigDecimal.ZERO;
            BigDecimal net = BigDecimal.ZERO;
            BigDecimal overUnder = BigDecimal.ZERO;

            for (OperationSectionRowDto r : rows) {
                OperationTotalsDto item = r.getData().get(k);
                if (item != null) {
                    tickets += item.getTickets();
                    monetary = monetary.add(item.getMonetaryAmt());
                    banks = banks.add(item.getBanks());
                    expected = expected.add(item.getExpectedValue());
                    net = net.add(item.getNetSales());
                    overUnder = overUnder.add(item.getOverUnder());
                }
            }

            totals.put(k, OperationTotalsDto.builder()
                .tickets(tickets)
                .monetaryAmt(monetary)
                .banks(banks)
                .expectedValue(expected)
                .netSales(net)
                .overUnder(overUnder)
                .build());
        }

        return totals;
    }

    private int parseSessionNum(String shiftName) {
        if (shiftName == null) return -1;
        String clean = shiftName.replaceAll("\\D", "");
        if (clean.isBlank()) return -1;
        try {
            return Integer.parseInt(clean);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public record GateFinancials(
        List<DeviceReportDto> devices,
        GateTotalsDto totals,
        GateBankSummaryDto bankSummary,
        GateSalesSummaryDto salesSummary,
        Map<String, OperationTotalsDto> operationSections
    ) {}

    public GateFinancials computeGateFinancials(
        Gate gate,
        List<CashBank> gateBanks,
        List<TicketBook> gateBooks,
        List<CreditReport> gateCredits,
        BigDecimal price,
        BigDecimal fee
    ) {
        // 1. Filter only assigned & returned/used assets per verified rules
        List<CashBank> assignedBanks = gateBanks.stream()
            .filter(b -> b.getStatus() != null && !"Available".equalsIgnoreCase(b.getStatus()))
            .collect(Collectors.toList());

        List<CashBank> usedBanks = gateBanks.stream()
            .filter(b -> "Returned".equalsIgnoreCase(b.getStatus()) && b.getOfficialCount() != null)
            .collect(Collectors.toList());

        List<TicketBook> usedBooks = gateBooks.stream()
            .filter(b -> b.getStatus() != null &&
                (b.getStatus().equalsIgnoreCase("Returned") || b.getStatus().equalsIgnoreCase("COMPLETED")) &&
                b.getNextTicket() != null && b.getStartNum() != null && b.getNextTicket() > b.getStartNum())
            .collect(Collectors.toList());

        // Deduplicate credit reports per deviceId (highest positive amount)
        Map<String, CreditReport> creditByDevice = new HashMap<>();
        for (CreditReport cr : gateCredits) {
            if (cr.getTotalAmount() == null || cr.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) continue;
            String devKey = getDeviceKey(cr);
            CreditReport existing = creditByDevice.get(devKey);
            if (existing == null || cr.getTotalAmount().compareTo(existing.getTotalAmount()) > 0) {
                creditByDevice.put(devKey, cr);
            }
        }
        List<CreditReport> validCredits = new ArrayList<>(creditByDevice.values());

        // Set of credit device keys
        Set<String> creditDeviceKeys = new HashSet<>(creditByDevice.keySet());

        // Helper mutable device representation
        class MutableDevice {
            String deviceId;
            boolean isCredit;
            BigDecimal bankAmount = BigDecimal.ZERO;
            List<BigDecimal> rawCashCounts = new ArrayList<>();
            BigDecimal ccAmount = BigDecimal.ZERO;
            int ticketsSold = 0;
        }

        Map<String, MutableDevice> deviceMap = new LinkedHashMap<>();

        // Helper to get or create device
        java.util.function.Function<String, MutableDevice> getOrCreate = key ->
            deviceMap.computeIfAbsent(key, k -> {
                MutableDevice d = new MutableDevice();
                d.deviceId = k;
                d.isCredit = creditDeviceKeys.contains(k);
                return d;
            });

        // 1. Process Cash Banks (assigned to cash devices)
        for (CashBank cb : usedBanks) {
            String key = getDeviceKey(cb);
            MutableDevice d = getOrCreate.apply(key);
            if (cb.getStartingAmount() != null) {
                d.bankAmount = d.bankAmount.add(cb.getStartingAmount());
            }
            if (cb.getOfficialCount() != null) {
                d.rawCashCounts.add(cb.getOfficialCount());
            }
        }

        // 2. Process Credit Reports (strictly credit devices, $0.00 starting bank)
        for (CreditReport cr : validCredits) {
            String key = getDeviceKey(cr);
            MutableDevice d = getOrCreate.apply(key);
            d.isCredit = true;
            d.ccAmount = cr.getTotalAmount();
        }

        // 3. Process Ticket Books
        for (TicketBook tb : usedBooks) {
            String key = getDeviceKey(tb);
            MutableDevice d = getOrCreate.apply(key);
            int sold = tb.getNextTicket() - tb.getStartNum();
            d.ticketsSold += sold;
        }

        // Calculate per-device metrics
        List<DeviceReportDto> deviceList = new ArrayList<>();
        for (MutableDevice d : deviceMap.values()) {
            boolean isCredit = d.isCredit;
            BigDecimal devBankAmount = isCredit ? BigDecimal.ZERO : d.bankAmount;
            BigDecimal ticketVal = isCredit
                ? BigDecimal.valueOf(d.ticketsSold).multiply(price.add(fee)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(d.ticketsSold).multiply(price).setScale(2, RoundingMode.HALF_UP);
            BigDecimal ticketBankVal = ticketVal.add(devBankAmount);

            // Resolve cashReturned for cash devices:
            // If multiple cash banks share the exact same official count, take it once (authoritative batch total)
            BigDecimal devCashCcAmount;
            if (isCredit) {
                devCashCcAmount = d.ccAmount;
            } else {
                if (d.rawCashCounts.isEmpty()) {
                    devCashCcAmount = BigDecimal.ZERO;
                } else {
                    Set<BigDecimal> uniqueCounts = new HashSet<>(d.rawCashCounts);
                    if (uniqueCounts.size() == 1) {
                        devCashCcAmount = d.rawCashCounts.get(0);
                    } else {
                        devCashCcAmount = d.rawCashCounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                    }
                }
            }

            BigDecimal difference = devCashCcAmount.subtract(ticketBankVal);

            deviceList.add(DeviceReportDto.builder()
                .deviceId(d.deviceId)
                .isCredit(isCredit)
                .bankAmount(devBankAmount)
                .ticketsSold(d.ticketsSold)
                .ticketValue(ticketVal)
                .ticketBankValue(ticketBankVal)
                .cashCcAmount(devCashCcAmount)
                .difference(difference)
                .build());
        }

        deviceList.sort((a, b) -> a.getDeviceId().compareToIgnoreCase(b.getDeviceId()));

        // Gate Totals
        BigDecimal gateBankAmount = BigDecimal.ZERO;
        int gateTicketsSold = 0;
        BigDecimal gateTicketValue = BigDecimal.ZERO;
        BigDecimal gateTicketBankValue = BigDecimal.ZERO;
        BigDecimal gateCashCcAmount = BigDecimal.ZERO;
        BigDecimal gateDifference = BigDecimal.ZERO;

        for (DeviceReportDto dev : deviceList) {
            gateBankAmount = gateBankAmount.add(dev.getBankAmount());
            gateTicketsSold += dev.getTicketsSold();
            gateTicketValue = gateTicketValue.add(dev.getTicketValue());
            gateTicketBankValue = gateTicketBankValue.add(dev.getTicketBankValue());
            gateCashCcAmount = gateCashCcAmount.add(dev.getCashCcAmount());
            gateDifference = gateDifference.add(dev.getDifference());
        }

        GateTotalsDto totals = GateTotalsDto.builder()
            .bankAmount(gateBankAmount)
            .ticketsSold(gateTicketsSold)
            .ticketValue(gateTicketValue)
            .ticketBankValue(gateTicketBankValue)
            .cashCcAmount(gateCashCcAmount)
            .difference(gateDifference)
            .build();

        // Bank Summary
        int banksIssued = assignedBanks.size();
        int banksUsed = usedBanks.size();
        int banksNotUsed = Math.max(0, banksIssued - banksUsed);

        GateBankSummaryDto bankSum = GateBankSummaryDto.builder()
            .gateId(gate.getGateId())
            .gateName(gate.getName())
            .displayOrder(gate.getDisplayOrder() != null ? gate.getDisplayOrder() : 9999)
            .issued(banksIssued)
            .used(banksUsed)
            .notUsed(banksNotUsed)
            .build();

        // Sales Summary
        int cashQty = 0;
        BigDecimal cashVal = BigDecimal.ZERO;
        BigDecimal cashReturned = BigDecimal.ZERO;
        BigDecimal cashBanksAmt = BigDecimal.ZERO;

        int ccQty = 0;
        BigDecimal ccVal = BigDecimal.ZERO;
        BigDecimal ccMonetaryAmt = BigDecimal.ZERO;

        for (DeviceReportDto dev : deviceList) {
            if (dev.isCredit()) {
                ccQty += dev.getTicketsSold();
                ccVal = ccVal.add(dev.getTicketValue());
                ccMonetaryAmt = ccMonetaryAmt.add(dev.getCashCcAmount());
            } else {
                cashQty += dev.getTicketsSold();
                cashVal = cashVal.add(dev.getTicketValue());
                cashReturned = cashReturned.add(dev.getCashCcAmount());
                cashBanksAmt = cashBanksAmt.add(dev.getBankAmount());
            }
        }

        GateSalesSummaryDto salesSum = GateSalesSummaryDto.builder()
            .gateId(gate.getGateId())
            .gateName(gate.getName())
            .displayOrder(gate.getDisplayOrder() != null ? gate.getDisplayOrder() : 9999)
            .cashQty(cashQty)
            .cashValue(cashVal)
            .ccQty(ccQty)
            .ccValue(ccVal)
            .totalQty(cashQty + ccQty)
            .totalValue(cashVal.add(ccVal))
            .build();

        // Operation sections
        BigDecimal cashNetSales = cashReturned.subtract(cashBanksAmt);
        BigDecimal cashOverUnder = cashNetSales.subtract(cashVal);

        BigDecimal creditNetSales = ccMonetaryAmt;
        BigDecimal creditOverUnder = creditNetSales.subtract(ccVal);

        BigDecimal combinedNetSales = gateCashCcAmount.subtract(gateBankAmount);
        BigDecimal combinedOverUnder = gateDifference;

        OperationTotalsDto cashSec = OperationTotalsDto.builder()
            .tickets(cashQty)
            .monetaryAmt(cashReturned)
            .banks(cashBanksAmt)
            .expectedValue(cashVal)
            .netSales(cashNetSales)
            .overUnder(cashOverUnder)
            .build();

        OperationTotalsDto creditSec = OperationTotalsDto.builder()
            .tickets(ccQty)
            .monetaryAmt(ccMonetaryAmt)
            .banks(BigDecimal.ZERO)
            .expectedValue(ccVal)
            .netSales(creditNetSales)
            .overUnder(creditOverUnder)
            .build();

        OperationTotalsDto combinedSec = OperationTotalsDto.builder()
            .tickets(gateTicketsSold)
            .monetaryAmt(gateCashCcAmount)
            .banks(gateBankAmount)
            .expectedValue(gateTicketValue)
            .netSales(combinedNetSales)
            .overUnder(combinedOverUnder)
            .build();

        Map<String, OperationTotalsDto> sections = Map.of(
            "cash", cashSec,
            "credit", creditSec,
            "combined", combinedSec
        );

        return new GateFinancials(deviceList, totals, bankSum, salesSum, sections);
    }

    private String getDeviceKey(CashBank cb) {
        if (cb.getDeviceId() != null && !cb.getDeviceId().isBlank()) {
            return cb.getDeviceId().trim();
        }
        if (cb.getBankName() != null && !cb.getBankName().isBlank()) {
            return cb.getBankName().trim();
        }
        return "General";
    }

    private String getDeviceKey(TicketBook tb) {
        if (tb.getDeviceId() != null && !tb.getDeviceId().isBlank()) {
            return tb.getDeviceId().trim();
        }
        if (tb.getScanCode() != null && !tb.getScanCode().isBlank()) {
            return tb.getScanCode().trim();
        }
        return "General";
    }

    private String getDeviceKey(CreditReport cr) {
        if (cr.getDeviceId() != null && !cr.getDeviceId().isBlank()) {
            return cr.getDeviceId().trim();
        }
        if (cr.getCreditReportId() != null) {
            return "Credit-" + cr.getCreditReportId().toString();
        }
        return "Credit-General";
    }
}
