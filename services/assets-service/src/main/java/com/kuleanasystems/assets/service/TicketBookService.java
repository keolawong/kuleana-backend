package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.dto.MaxTicketsSoldProjection;
import com.kuleanasystems.assets.dto.TicketBookDto;
import com.kuleanasystems.assets.dto.TicketBookPatchDto;
import com.kuleanasystems.assets.entity.Gate;
import com.kuleanasystems.assets.entity.Operation;
import com.kuleanasystems.assets.entity.Shift;
import com.kuleanasystems.assets.entity.TicketBook;
import com.kuleanasystems.assets.entity.Person;
import com.kuleanasystems.assets.entity.PersonSticker;
import com.kuleanasystems.assets.entity.TicketBookHistory;
import com.kuleanasystems.assets.repository.GateRepository;
import com.kuleanasystems.assets.repository.OperationRepository;
import com.kuleanasystems.assets.repository.ShiftRepository;
import com.kuleanasystems.assets.repository.TicketBookHistoryRepository;
import com.kuleanasystems.assets.repository.TicketBookRepository;
import com.kuleanasystems.assets.repository.TicketBookSpecification;
import com.kuleanasystems.assets.repository.PersonRepository;
import com.kuleanasystems.assets.repository.PersonStickerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketBookService {

    private final TicketBookRepository ticketBookRepository;
    private final TicketBookHistoryRepository ticketBookHistoryRepository;
    private final OperationRepository operationRepository;
    private final ShiftRepository shiftRepository;
    private final GateRepository gateRepository;
    private final PersonRepository personRepository;
    private final PersonStickerRepository personStickerRepository;

    private Person resolvePerson(UUID personOrStickerId) {
        if (personOrStickerId == null) {
            return null;
        }
        return personRepository.findById(personOrStickerId)
                .or(() -> personStickerRepository.findById(personOrStickerId).map(PersonSticker::getPerson))
                .orElseThrow(() -> new EntityNotFoundException("Person or PersonSticker not found with id: " + personOrStickerId));
    }

    @Transactional
    public TicketBook createTicketBook(TicketBookDto dto) {
        TicketBook ticketBook = new TicketBook();
        ticketBook.setScanCode(dto.getScanCode());
        ticketBook.setStartNum(dto.getStartNum());
        ticketBook.setEndNum(dto.getEndNum());
        ticketBook.setNextTicket(dto.getNextTicket());
        if (dto.getIsReusable() != null) ticketBook.setIsReusable(dto.getIsReusable());
        ticketBook.setStatus(dto.getStatus());
        ticketBook.setDeviceId(dto.getDeviceId());
        ticketBook.setParentTicketBookId(dto.getParentTicketBookId());
        ticketBook.setProcessId(dto.getProcessId());

        if (dto.getOperationId() != null) {
            Operation operation = operationRepository.findById(dto.getOperationId())
                    .orElseThrow(() -> new EntityNotFoundException("Operation not found with id: " + dto.getOperationId()));
            ticketBook.setOperation(operation);
        }
        if (dto.getShiftId() != null) {
            Shift shift = shiftRepository.findById(dto.getShiftId())
                    .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + dto.getShiftId()));
            ticketBook.setShift(shift);
        }
        if (dto.getGateId() != null) {
            Gate gate = gateRepository.findById(dto.getGateId())
                    .orElseThrow(() -> new EntityNotFoundException("Gate not found with id: " + dto.getGateId()));
            ticketBook.setGate(gate);
        }
        UUID targetPersonId = dto.getPersonStickerId();
        if (targetPersonId != null) {
            ticketBook.setPerson(resolvePerson(targetPersonId));
        }

        String currentUser = "System";
        ticketBook.setCreatedBy(currentUser);

        TicketBook saved = ticketBookRepository.save(ticketBook);
        saveTicketBookHistory(saved);
        return saved;
    }

    @Transactional
    public List<TicketBook> createTicketBooks(List<TicketBookDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> operationIds = dtos.stream().map(TicketBookDto::getOperationId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> shiftIds = dtos.stream().map(TicketBookDto::getShiftId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> gateIds = dtos.stream().map(TicketBookDto::getGateId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> personIds = dtos.stream().map(TicketBookDto::getPersonStickerId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<UUID, Operation> operations = operationIds.isEmpty() ? Collections.emptyMap() :
                operationRepository.findAllById(operationIds).stream()
                        .collect(Collectors.toMap(Operation::getOperationId, Function.identity()));
        Map<UUID, Shift> shifts = shiftIds.isEmpty() ? Collections.emptyMap() :
                shiftRepository.findAllById(shiftIds).stream()
                        .collect(Collectors.toMap(Shift::getShiftId, Function.identity()));
        Map<UUID, Gate> gates = gateIds.isEmpty() ? Collections.emptyMap() :
                gateRepository.findAllById(gateIds).stream()
                        .collect(Collectors.toMap(Gate::getGateId, Function.identity()));
        Map<UUID, Person> persons = new HashMap<>();
        if (!personIds.isEmpty()) {
            personRepository.findAllById(personIds).forEach(p -> persons.put(p.getPersonId(), p));
            Set<UUID> unresolved = personIds.stream().filter(id -> !persons.containsKey(id)).collect(Collectors.toSet());
            if (!unresolved.isEmpty()) {
                personStickerRepository.findAllById(unresolved).forEach(ps -> {
                    if (ps.getPerson() != null) {
                        persons.put(ps.getPersonStickerId(), ps.getPerson());
                    }
                });
            }
        }

        String currentUser = "System";
        List<TicketBook> ticketBooks = dtos.stream().map(dto -> {
            TicketBook ticketBook = new TicketBook();
            ticketBook.setScanCode(dto.getScanCode());
            ticketBook.setStartNum(dto.getStartNum());
            ticketBook.setEndNum(dto.getEndNum());
            ticketBook.setNextTicket(dto.getNextTicket());
            if (dto.getIsReusable() != null) ticketBook.setIsReusable(dto.getIsReusable());
            ticketBook.setStatus(dto.getStatus());
            ticketBook.setDeviceId(dto.getDeviceId());
            ticketBook.setParentTicketBookId(dto.getParentTicketBookId());
            ticketBook.setProcessId(dto.getProcessId());

            if (dto.getOperationId() != null) {
                Operation operation = operations.get(dto.getOperationId());
                if (operation == null) {
                    throw new EntityNotFoundException("Operation not found with id: " + dto.getOperationId());
                }
                ticketBook.setOperation(operation);
            }
            if (dto.getShiftId() != null) {
                Shift shift = shifts.get(dto.getShiftId());
                if (shift == null) {
                    throw new EntityNotFoundException("Shift not found with id: " + dto.getShiftId());
                }
                ticketBook.setShift(shift);
            }
            if (dto.getGateId() != null) {
                Gate gate = gates.get(dto.getGateId());
                if (gate == null) {
                    throw new EntityNotFoundException("Gate not found with id: " + dto.getGateId());
                }
                ticketBook.setGate(gate);
            }
            if (dto.getPersonStickerId() != null) {
                Person person = persons.get(dto.getPersonStickerId());
                if (person == null) {
                    throw new EntityNotFoundException("Person or PersonSticker not found with id: " + dto.getPersonStickerId());
                }
                ticketBook.setPerson(person);
            }

            ticketBook.setCreatedBy(currentUser);
            return ticketBook;
        }).collect(Collectors.toList());

        List<TicketBook> savedBooks = ticketBookRepository.saveAll(ticketBooks);
        savedBooks.forEach(this::saveTicketBookHistory);
        return savedBooks;
    }

    @Transactional
    public List<TicketBook> splitTicketBooks(List<UUID> ticketBookIds) {
        String currentUser = "System";
        List<TicketBook> result = new java.util.ArrayList<>();

        for (UUID ticketBookId : ticketBookIds) {
            TicketBook oldTicketBook = ticketBookRepository.findById(ticketBookId)
                    .orElseThrow(() -> new EntityNotFoundException("TicketBook not found with id: " + ticketBookId));

            if (oldTicketBook.getNextTicket() == null || 
                oldTicketBook.getNextTicket() > oldTicketBook.getEndNum() || 
                oldTicketBook.getNextTicket() <= oldTicketBook.getStartNum()) {
                continue;
            }

            TicketBook newTicketBook = new TicketBook();
            newTicketBook.setScanCode(oldTicketBook.getScanCode());
            newTicketBook.setStartNum(oldTicketBook.getNextTicket());
            newTicketBook.setEndNum(oldTicketBook.getEndNum());
            newTicketBook.setNextTicket(null);
            newTicketBook.setIsReusable(oldTicketBook.getIsReusable());
            newTicketBook.setStatus("AVAILABLE");
            newTicketBook.setParentTicketBookId(oldTicketBook.getTicketBookId());
            newTicketBook.setOperation(oldTicketBook.getOperation());
            newTicketBook.setCreatedBy(currentUser);

            oldTicketBook.setEndNum(oldTicketBook.getNextTicket() - 1);
            oldTicketBook.setStatus("COMPLETED");
            oldTicketBook.setUpdatedBy(currentUser);

            ticketBookRepository.save(oldTicketBook);
            TicketBook savedNew = ticketBookRepository.save(newTicketBook);

            saveTicketBookHistory(oldTicketBook);
            saveTicketBookHistory(savedNew);

            result.add(savedNew);
        }
        return result;
    }

    @Transactional
    public TicketBook updateTicketBook(UUID ticketBookId, TicketBookPatchDto dto) {
        TicketBook ticketBook = ticketBookRepository.findById(ticketBookId)
                .orElseThrow(() -> new EntityNotFoundException("TicketBook not found with id: " + ticketBookId));

        if (dto.isDirty("scanCode")) ticketBook.setScanCode(dto.getScanCode());
        if (dto.isDirty("startNum")) {
            if (dto.getStartNum() != null && dto.getStartNum() < 0) {
                throw new IllegalArgumentException("startNum must be greater than or equal to 0.");
            }
            ticketBook.setStartNum(dto.getStartNum());
        }
        if (dto.isDirty("endNum")) {
            if (dto.getEndNum() != null && dto.getEndNum() < 0) {
                throw new IllegalArgumentException("endNum must be greater than or equal to 0.");
            }
            ticketBook.setEndNum(dto.getEndNum());
        }

        if (dto.isDirty("startNum") || dto.isDirty("endNum")) {
            if (ticketBook.getStartNum() != null && ticketBook.getEndNum() != null && ticketBook.getEndNum() < ticketBook.getStartNum()) {
                throw new IllegalArgumentException("endNum must be greater than or equal to startNum.");
            }
        }
        if (dto.isDirty("nextTicket")) ticketBook.setNextTicket(dto.getNextTicket());
        if (dto.isDirty("isReusable")) ticketBook.setIsReusable(dto.getIsReusable());
        if (dto.isDirty("status")) ticketBook.setStatus(dto.getStatus());
        if (dto.isDirty("deviceId")) ticketBook.setDeviceId(dto.getDeviceId());
        if (dto.isDirty("parentTicketBookId")) ticketBook.setParentTicketBookId(dto.getParentTicketBookId());
        if (dto.isDirty("processId")) ticketBook.setProcessId(dto.getProcessId());

        if (dto.isDirty("operationId")) {
            if (dto.getOperationId() == null) {
                ticketBook.setOperation(null);
            } else {
                Operation operation = operationRepository.findById(dto.getOperationId())
                        .orElseThrow(() -> new EntityNotFoundException("Operation not found with id: " + dto.getOperationId()));
                ticketBook.setOperation(operation);
            }
        }

        if (dto.isDirty("shiftId")) {
            if (dto.getShiftId() == null) {
                ticketBook.setShift(null);
            } else {
                Shift shift = shiftRepository.findById(dto.getShiftId())
                        .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + dto.getShiftId()));
                ticketBook.setShift(shift);
            }
        }

        if (dto.isDirty("gateId")) {
            if (dto.getGateId() == null) {
                ticketBook.setGate(null);
            } else {
                Gate gate = gateRepository.findById(dto.getGateId())
                        .orElseThrow(() -> new EntityNotFoundException("Gate not found with id: " + dto.getGateId()));
                ticketBook.setGate(gate);
            }
        }

        if (dto.isDirty("personStickerId")) {
            if (dto.getPersonStickerId() == null) {
                ticketBook.setPerson(null);
            } else {
                ticketBook.setPerson(resolvePerson(dto.getPersonStickerId()));
            }
        }

        validateSplitCascade(ticketBook, dto);

        String currentUser = "System";
        ticketBook.setUpdatedBy(currentUser);

        TicketBook saved = ticketBookRepository.save(ticketBook);
        saveTicketBookHistory(saved);

        cascadeSplitTicketBookChanges(saved, dto, currentUser);

        return saved;
    }

    private void validateSplitCascade(TicketBook ticketBook, TicketBookPatchDto dto) {
        if (dto.isDirty("startNum") && ticketBook.getParentTicketBookId() != null) {
            TicketBook parent = ticketBookRepository.findById(ticketBook.getParentTicketBookId()).orElse(null);
            if (parent != null) {
                int newParentEndNum = ticketBook.getStartNum() - 1;
                if (parent.getStartNum() != null && newParentEndNum < parent.getStartNum()) {
                    throw new IllegalArgumentException("Adjusted start number would cause parent ticket book end number (" + newParentEndNum + ") to be less than its start number (" + parent.getStartNum() + ").");
                }
            }
        }

        if (dto.isDirty("endNum")) {
            List<TicketBook> children = ticketBookRepository.findByParentTicketBookId(ticketBook.getTicketBookId());
            for (TicketBook child : children) {
                int newChildStartNum = ticketBook.getEndNum() + 1;
                if (child.getEndNum() != null && newChildStartNum > child.getEndNum()) {
                    throw new IllegalArgumentException("Adjusted end number would cause child split ticket book start number (" + newChildStartNum + ") to exceed its end number (" + child.getEndNum() + ").");
                }
            }
        }
    }

    private void cascadeSplitTicketBookChanges(TicketBook ticketBook, TicketBookPatchDto dto, String currentUser) {
        if (dto.isDirty("startNum") && ticketBook.getParentTicketBookId() != null) {
            TicketBook parent = ticketBookRepository.findById(ticketBook.getParentTicketBookId()).orElse(null);
            if (parent != null) {
                int newParentEndNum = ticketBook.getStartNum() - 1;
                if (parent.getStartNum() != null && newParentEndNum < parent.getStartNum()) {
                    throw new IllegalArgumentException("Adjusted start number would cause parent ticket book end number (" + newParentEndNum + ") to be less than its start number (" + parent.getStartNum() + ").");
                }
                parent.setEndNum(newParentEndNum);
                parent.setUpdatedBy(currentUser);
                ticketBookRepository.save(parent);
                saveTicketBookHistory(parent);
            }
        }

        if (dto.isDirty("endNum")) {
            List<TicketBook> children = ticketBookRepository.findByParentTicketBookId(ticketBook.getTicketBookId());
            for (TicketBook child : children) {
                int newChildStartNum = ticketBook.getEndNum() + 1;
                if (child.getEndNum() != null && newChildStartNum > child.getEndNum()) {
                    throw new IllegalArgumentException("Adjusted end number would cause child split ticket book start number (" + newChildStartNum + ") to exceed its end number (" + child.getEndNum() + ").");
                }
                child.setStartNum(newChildStartNum);
                child.setUpdatedBy(currentUser);
                ticketBookRepository.save(child);
                saveTicketBookHistory(child);
            }
        }

        if (dto.isDirty("scanCode")) {
            cascadeScanCodeAcrossSplitLineage(ticketBook, dto.getScanCode(), currentUser);
        }
    }

    private void cascadeScanCodeAcrossSplitLineage(TicketBook current, String newScanCode, String currentUser) {
        if (newScanCode == null || newScanCode.isBlank()) return;

        TicketBook root = current;
        while (root.getParentTicketBookId() != null) {
            Optional<TicketBook> parentOpt = ticketBookRepository.findById(root.getParentTicketBookId());
            if (parentOpt.isPresent()) {
                root = parentOpt.get();
            } else {
                break;
            }
        }

        Set<UUID> visited = new HashSet<>();
        Queue<TicketBook> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            TicketBook node = queue.poll();
            if (visited.add(node.getTicketBookId())) {
                if (!node.getTicketBookId().equals(current.getTicketBookId())) {
                    node.setScanCode(newScanCode);
                    node.setUpdatedBy(currentUser);
                    ticketBookRepository.save(node);
                    saveTicketBookHistory(node);
                }
                List<TicketBook> children = ticketBookRepository.findByParentTicketBookId(node.getTicketBookId());
                queue.addAll(children);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<TicketBook> searchTicketBooks(
            UUID ticketBookId, String scanCode, Integer startNum, Integer endNum,
            Integer nextTicket, Boolean isReusable, String status, UUID operationId,
            UUID shiftId, UUID gateId, String deviceId, UUID parentTicketBookId,
            UUID processId, UUID personStickerId, String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) {
        Specification<TicketBook> spec = TicketBookSpecification.search(
                ticketBookId, scanCode, startNum, endNum, nextTicket, isReusable, status,
                operationId, shiftId, gateId, deviceId, parentTicketBookId, processId, personStickerId,
                createdBy, createdAt, updatedBy, updatedAt);
        return ticketBookRepository.findAll(spec);
    }

    @Transactional(readOnly = true)
    public Integer getMaxTicketsSoldMetrics(UUID excludeOperationId, String shiftName, String gateName) {
        MaxTicketsSoldProjection projection = ticketBookRepository.findMaxTicketsSoldByMetrics(excludeOperationId, shiftName, gateName);
        return (projection != null && projection.getMaxTicketsSold() != null) ? projection.getMaxTicketsSold() : 0;
    }

    private void saveTicketBookHistory(TicketBook ticketBook) {
        TicketBookHistory history = new TicketBookHistory();
        history.setTicketBookId(ticketBook.getTicketBookId());
        history.setScanCode(ticketBook.getScanCode());
        history.setStartNum(ticketBook.getStartNum());
        history.setEndNum(ticketBook.getEndNum());
        history.setNextTicket(ticketBook.getNextTicket());
        history.setIsReusable(ticketBook.getIsReusable());
        history.setStatus(ticketBook.getStatus());
        history.setOperation(ticketBook.getOperation());
        history.setShift(ticketBook.getShift());
        history.setGate(ticketBook.getGate());
        history.setDeviceId(ticketBook.getDeviceId());
        history.setParentTicketBookId(ticketBook.getParentTicketBookId());
        history.setProcessId(ticketBook.getProcessId());
        history.setCreatedBy(ticketBook.getUpdatedBy() != null ? ticketBook.getUpdatedBy() : ticketBook.getCreatedBy());
        ticketBookHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<TicketBookHistory> getHistoryByShiftId(UUID shiftId) {
        return ticketBookHistoryRepository.findByShift_ShiftId(shiftId);
    }

    @Transactional(readOnly = true)
    public List<TicketBook> getTicketBooksByIds(List<UUID> ids) {
        return ticketBookRepository.findAllById(ids);
    }

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
