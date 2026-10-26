package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.dto.TicketBookDto;
import com.kuleanasystems.assets.dto.TicketBookPatchDto;
import com.kuleanasystems.assets.entity.TicketBook;
import com.kuleanasystems.assets.service.TicketBookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.kuleanasystems.assets.entity.TicketBookHistory;

@RestController
@RequestMapping("/api/ticket-books")
@RequiredArgsConstructor
public class TicketBookController {

    private final TicketBookService ticketBookService;

    @PostMapping
    public ResponseEntity<TicketBook> createTicketBook(@Valid @RequestBody TicketBookDto ticketBookDto) {
        TicketBook createdTicketBook = ticketBookService.createTicketBook(ticketBookDto);
        return ResponseEntity.created(URI.create("/api/ticket-books/" + createdTicketBook.getTicketBookId())).body(createdTicketBook);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<TicketBook>> createTicketBooks(@Valid @RequestBody List<TicketBookDto> ticketBookDtos) {
        List<TicketBook> createdTicketBooks = ticketBookService.createTicketBooks(ticketBookDtos);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTicketBooks);
    }

    @PostMapping("/bulk-split")
    public ResponseEntity<List<TicketBook>> splitTicketBooks(@RequestBody List<UUID> ticketBookIds) {
        List<TicketBook> splitBooks = ticketBookService.splitTicketBooks(ticketBookIds);
        return ResponseEntity.ok(splitBooks);
    }

    @PatchMapping("/{ticketBookId}")
    public ResponseEntity<TicketBook> updateTicketBook(@PathVariable UUID ticketBookId, @RequestBody TicketBookPatchDto ticketBookPatchDto) {
        TicketBook updatedTicketBook = ticketBookService.updateTicketBook(ticketBookId, ticketBookPatchDto);
        return ResponseEntity.ok(updatedTicketBook);
    }

    @GetMapping
    public ResponseEntity<List<TicketBook>> getTicketBooks(
            @RequestParam(required = false) UUID ticketBookId,
            @RequestParam(required = false) String scanCode,
            @RequestParam(required = false) Integer startNum,
            @RequestParam(required = false) Integer endNum,
            @RequestParam(required = false) Integer nextTicket,
            @RequestParam(required = false) Boolean isReusable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID operationId,
            @RequestParam(required = false) UUID shiftId,
            @RequestParam(required = false) UUID gateId,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) UUID parentTicketBookId,
            @RequestParam(required = false) UUID processId,
            @RequestParam(required = false) UUID personStickerId,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Instant createdAt,
            @RequestParam(required = false) String updatedBy,
            @RequestParam(required = false) Instant updatedAt) {

        List<TicketBook> ticketBooks = ticketBookService.searchTicketBooks(
                ticketBookId, scanCode, startNum, endNum, nextTicket, isReusable, status,
                operationId, shiftId, gateId, deviceId, parentTicketBookId, processId, personStickerId,
                createdBy, createdAt, updatedBy, updatedAt);

        if (shiftId != null) {
            List<TicketBookHistory> historyList = ticketBookService.getHistoryByShiftId(shiftId);
            
            if (gateId != null) {
                historyList = historyList.stream()
                        .filter(h -> h.getGate() != null && h.getGate().getGateId().equals(gateId))
                        .collect(Collectors.toList());
            }
            
            Map<UUID, TicketBookHistory> latestHistoryMap = historyList.stream()
                .collect(Collectors.toMap(
                    TicketBookHistory::getTicketBookId,
                    h -> h,
                    (h1, h2) -> h1.getCreatedAt().isAfter(h2.getCreatedAt()) ? h1 : h2
                ));

            List<TicketBook> mergedList = new ArrayList<>(ticketBooks);
            final List<TicketBook> activeTicketBooks = ticketBooks;
            
            List<UUID> inactiveIds = latestHistoryMap.values().stream()
                .map(TicketBookHistory::getTicketBookId)
                .filter(id -> activeTicketBooks.stream().noneMatch(tb -> tb.getTicketBookId().equals(id)))
                .collect(Collectors.toList());

            Map<UUID, TicketBook> currentBookMap = new java.util.HashMap<>();
            if (!inactiveIds.isEmpty()) {
                currentBookMap = ticketBookService.getTicketBooksByIds(inactiveIds).stream()
                    .collect(Collectors.toMap(TicketBook::getTicketBookId, tb -> tb));
            }

            for (TicketBookHistory history : latestHistoryMap.values()) {
                boolean isActive = activeTicketBooks.stream().anyMatch(tb -> tb.getTicketBookId().equals(history.getTicketBookId()));
                
                if (!isActive) {
                    TicketBook currentTb = currentBookMap.get(history.getTicketBookId());
                    if (currentTb != null && "Available".equals(currentTb.getStatus())) {
                        continue;
                    }

                    boolean hasReturnedStatus = historyList.stream()
                        .filter(h -> h.getTicketBookId().equals(history.getTicketBookId()))
                        .anyMatch(h -> "Returned".equals(h.getStatus()) || "Returned Unused".equals(h.getStatus()));
                        
                    if (hasReturnedStatus) {
                        TicketBook historicalTb = new TicketBook();
                        historicalTb.setTicketBookId(history.getTicketBookId());
                        historicalTb.setScanCode(history.getScanCode());
                        historicalTb.setStartNum(history.getStartNum());
                        historicalTb.setEndNum(history.getEndNum());
                        historicalTb.setNextTicket(history.getNextTicket());
                        historicalTb.setIsReusable(history.getIsReusable());
                        historicalTb.setStatus(history.getStatus());
                        historicalTb.setOperation(history.getOperation());
                        historicalTb.setShift(history.getShift());
                        historicalTb.setGate(history.getGate());
                        historicalTb.setDeviceId(history.getDeviceId());
                        historicalTb.setParentTicketBookId(history.getParentTicketBookId());
                        historicalTb.setProcessId(history.getProcessId());
                        mergedList.add(historicalTb);
                    }
                }
            }
            ticketBooks = mergedList;
        }

        if (ticketBooks.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(ticketBooks);
    }

    @GetMapping("/max-sold/{excludeOperationId}/{shiftName}/{gateName}")
    public ResponseEntity<Integer> getMaxTicketsSold(
            @PathVariable UUID excludeOperationId,
            @PathVariable String shiftName,
            @PathVariable String gateName) {
        Integer maxTickets = ticketBookService.getMaxTicketsSoldMetrics(excludeOperationId, shiftName, gateName);
        return ResponseEntity.ok(maxTickets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketBook> getTicketBookById(@PathVariable UUID id) {
        return ticketBookService.getTicketBookById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/next-ticket")
    public ResponseEntity<TicketBook> updateNextTicket(@PathVariable UUID id, @RequestParam Integer nextTicket) {
        TicketBook updated = ticketBookService.updateNextTicket(id, nextTicket);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketBook> updateStatus(@PathVariable UUID id, @RequestParam String status) {
        TicketBook updated = ticketBookService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }
}
