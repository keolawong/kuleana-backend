package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.entity.TicketBook;
import com.kuleanasystems.assets.service.TicketBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ticket-books")
@RequiredArgsConstructor
public class TicketBookController {

    private final TicketBookService ticketBookService;

    @GetMapping
    public ResponseEntity<List<TicketBook>> getTicketBooks(
            @RequestParam(required = false) UUID operationId,
            @RequestParam(required = false) UUID shiftId,
            @RequestParam(required = false) String scanCode) {
        if (scanCode != null) {
            return ticketBookService.findByScanCode(scanCode)
                    .map(b -> ResponseEntity.ok(List.of(b)))
                    .orElse(ResponseEntity.ok(List.of()));
        }
        List<TicketBook> books = ticketBookService.getTicketBooks(operationId, shiftId);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketBook> getTicketBookById(@PathVariable UUID id) {
        return ticketBookService.getTicketBookById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TicketBook> createTicketBook(@RequestBody TicketBook ticketBook) {
        TicketBook saved = ticketBookService.saveTicketBook(ticketBook);
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/{id}/next-ticket")
    public ResponseEntity<TicketBook> updateNextTicket(@PathVariable UUID id, @RequestParam Integer nextTicket) {
        TicketBook updated = ticketBookService.updateNextTicket(id, nextTicket);
        return ResponseEntity.ok(updated);
    }
}
