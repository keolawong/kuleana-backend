package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.entity.TicketBook;
import com.kuleanasystems.assets.repository.TicketBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketBookService {

    private final TicketBookRepository ticketBookRepository;

    public List<TicketBook> getTicketBooks(UUID operationId, UUID shiftId) {
        if (operationId != null && shiftId != null) {
            return ticketBookRepository.findByOperationOperationIdAndShiftShiftId(operationId, shiftId);
        } else if (shiftId != null) {
            return ticketBookRepository.findByShiftShiftId(shiftId);
        } else if (operationId != null) {
            return ticketBookRepository.findByOperationOperationId(operationId);
        }
        return ticketBookRepository.findAll();
    }

    public Optional<TicketBook> getTicketBookById(UUID id) {
        return ticketBookRepository.findById(id);
    }

    public Optional<TicketBook> findByScanCode(String scanCode) {
        return ticketBookRepository.findByScanCode(scanCode);
    }

    @Transactional
    public TicketBook saveTicketBook(TicketBook ticketBook) {
        return ticketBookRepository.save(ticketBook);
    }

    @Transactional
    public TicketBook updateNextTicket(UUID id, Integer nextTicket) {
        TicketBook book = ticketBookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TicketBook not found: " + id));
        book.setNextTicket(nextTicket);
        return ticketBookRepository.save(book);
    }

    @Transactional
    public TicketBook updateStatus(UUID id, String status) {
        TicketBook book = ticketBookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TicketBook not found: " + id));
        book.setStatus(status);
        return ticketBookRepository.save(book);
    }
}
