package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.dto.CashBankDto;
import com.kuleanasystems.assets.dto.CashBankPatchDto;
import com.kuleanasystems.assets.entity.CashBank;
import com.kuleanasystems.assets.entity.Gate;
import com.kuleanasystems.assets.entity.Operation;
import com.kuleanasystems.assets.entity.Shift;
import com.kuleanasystems.assets.entity.Person;
import com.kuleanasystems.assets.entity.PersonSticker;
import com.kuleanasystems.assets.entity.CashBankHistory;
import com.kuleanasystems.assets.repository.CashBankHistoryRepository;
import com.kuleanasystems.assets.repository.CashBankRepository;
import com.kuleanasystems.assets.repository.CashBankSpecification;
import com.kuleanasystems.assets.repository.GateRepository;
import com.kuleanasystems.assets.repository.OperationRepository;
import com.kuleanasystems.assets.repository.ShiftRepository;
import com.kuleanasystems.assets.repository.PersonRepository;
import com.kuleanasystems.assets.repository.PersonStickerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CashBankService {

    private final CashBankRepository cashBankRepository;
    private final CashBankHistoryRepository cashBankHistoryRepository;
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
    public CashBank createCashBank(CashBankDto dto) {
        CashBank cashBank = new CashBank();
        cashBank.setBankName(dto.getBankName());
        cashBank.setStartingAmount(dto.getStartingAmount());
        cashBank.setOfficialCount(dto.getOfficialCount());
        if (dto.getCount100s() != null) cashBank.setCount100s(dto.getCount100s());
        if (dto.getCount50s() != null) cashBank.setCount50s(dto.getCount50s());
        if (dto.getCount20s() != null) cashBank.setCount20s(dto.getCount20s());
        if (dto.getCount10s() != null) cashBank.setCount10s(dto.getCount10s());
        if (dto.getCount5s() != null) cashBank.setCount5s(dto.getCount5s());
        if (dto.getCount1s() != null) cashBank.setCount1s(dto.getCount1s());
        if (dto.getCount1cs() != null) cashBank.setCount1cs(dto.getCount1cs());
        cashBank.setStatus(dto.getStatus());
        cashBank.setDeviceId(dto.getDeviceId());
        cashBank.setParentCashBankId(dto.getParentCashBankId());
        cashBank.setProcessId(dto.getProcessId());

        if (dto.getOperationId() != null) {
            Operation operation = operationRepository.findById(dto.getOperationId())
                    .orElseThrow(() -> new EntityNotFoundException("Operation not found with id: " + dto.getOperationId()));
            cashBank.setOperation(operation);
        }
        if (dto.getShiftId() != null) {
            Shift shift = shiftRepository.findById(dto.getShiftId())
                    .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + dto.getShiftId()));
            cashBank.setShift(shift);
        }
        if (dto.getGateId() != null) {
            Gate gate = gateRepository.findById(dto.getGateId())
                    .orElseThrow(() -> new EntityNotFoundException("Gate not found with id: " + dto.getGateId()));
            cashBank.setGate(gate);
        }
        UUID targetPersonId = dto.getPersonStickerId();
        if (targetPersonId != null) {
            cashBank.setPerson(resolvePerson(targetPersonId));
        }

        String currentUser = "System";
        cashBank.setCreatedBy(currentUser);

        CashBank saved = cashBankRepository.save(cashBank);
        saveCashBankHistory(saved);
        return saved;
    }

    @Transactional
    public List<CashBank> createCashBanks(List<CashBankDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> operationIds = dtos.stream().map(CashBankDto::getOperationId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> shiftIds = dtos.stream().map(CashBankDto::getShiftId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> gateIds = dtos.stream().map(CashBankDto::getGateId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> personIds = dtos.stream().map(CashBankDto::getPersonStickerId).filter(Objects::nonNull).collect(Collectors.toSet());

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
        List<CashBank> cashBanks = dtos.stream().map(dto -> {
            CashBank cashBank = new CashBank();
            cashBank.setBankName(dto.getBankName());
            cashBank.setStartingAmount(dto.getStartingAmount());
            cashBank.setOfficialCount(dto.getOfficialCount());
            if (dto.getCount100s() != null) cashBank.setCount100s(dto.getCount100s());
            if (dto.getCount50s() != null) cashBank.setCount50s(dto.getCount50s());
            if (dto.getCount20s() != null) cashBank.setCount20s(dto.getCount20s());
            if (dto.getCount10s() != null) cashBank.setCount10s(dto.getCount10s());
            if (dto.getCount5s() != null) cashBank.setCount5s(dto.getCount5s());
            if (dto.getCount1s() != null) cashBank.setCount1s(dto.getCount1s());
            if (dto.getCount1cs() != null) cashBank.setCount1cs(dto.getCount1cs());
            cashBank.setStatus(dto.getStatus());
            cashBank.setDeviceId(dto.getDeviceId());
            cashBank.setParentCashBankId(dto.getParentCashBankId());
            cashBank.setProcessId(dto.getProcessId());

            if (dto.getOperationId() != null) {
                Operation operation = operations.get(dto.getOperationId());
                if (operation == null) {
                    throw new EntityNotFoundException("Operation not found with id: " + dto.getOperationId());
                }
                cashBank.setOperation(operation);
            }
            if (dto.getShiftId() != null) {
                Shift shift = shifts.get(dto.getShiftId());
                if (shift == null) {
                    throw new EntityNotFoundException("Shift not found with id: " + dto.getShiftId());
                }
                cashBank.setShift(shift);
            }
            if (dto.getGateId() != null) {
                Gate gate = gates.get(dto.getGateId());
                if (gate == null) {
                    throw new EntityNotFoundException("Gate not found with id: " + dto.getGateId());
                }
                cashBank.setGate(gate);
            }
            if (dto.getPersonStickerId() != null) {
                Person person = persons.get(dto.getPersonStickerId());
                if (person == null) {
                    throw new EntityNotFoundException("Person or PersonSticker not found with id: " + dto.getPersonStickerId());
                }
                cashBank.setPerson(person);
            }
            cashBank.setCreatedBy(currentUser);
            return cashBank;
        }).collect(Collectors.toList());

        List<CashBank> savedBanks = cashBankRepository.saveAll(cashBanks);
        savedBanks.forEach(this::saveCashBankHistory);
        return savedBanks;
    }

    @Transactional
    public CashBank updateCashBank(UUID cashBankId, CashBankPatchDto dto) {
        CashBank cashBank = cashBankRepository.findById(cashBankId)
                .orElseThrow(() -> new EntityNotFoundException("CashBank not found with id: " + cashBankId));

        if (dto.isDirty("bankName") || dto.isDirty("startingAmount")) {
            if (cashBank.getStatus() != null && cashBank.getStatus().equalsIgnoreCase("ASSIGNED")) {
                throw new IllegalStateException("Cannot edit a cash bank that is currently assigned.");
            }

            boolean isUsedOrReconciled = cashBank.getOfficialCount() != null
                    || (cashBank.getCount100s() != null && cashBank.getCount100s() > 0)
                    || (cashBank.getCount50s() != null && cashBank.getCount50s() > 0)
                    || (cashBank.getCount20s() != null && cashBank.getCount20s() > 0)
                    || (cashBank.getCount10s() != null && cashBank.getCount10s() > 0)
                    || (cashBank.getCount5s() != null && cashBank.getCount5s() > 0)
                    || (cashBank.getCount1s() != null && cashBank.getCount1s() > 0)
                    || (cashBank.getCount1cs() != null && cashBank.getCount1cs() > 0)
                    || (cashBank.getStatus() != null && cashBank.getStatus().equalsIgnoreCase("RETURNED"));

            if (isUsedOrReconciled) {
                throw new IllegalStateException("Cannot edit a cash bank that has already been used or reconciled.");
            }
        }

        if (dto.isDirty("bankName")) cashBank.setBankName(dto.getBankName());
        if (dto.isDirty("startingAmount")) cashBank.setStartingAmount(dto.getStartingAmount());
        if (dto.isDirty("officialCount")) cashBank.setOfficialCount(dto.getOfficialCount());
        if (dto.isDirty("count100s")) cashBank.setCount100s(dto.getCount100s());
        if (dto.isDirty("count50s")) cashBank.setCount50s(dto.getCount50s());
        if (dto.isDirty("count20s")) cashBank.setCount20s(dto.getCount20s());
        if (dto.isDirty("count10s")) cashBank.setCount10s(dto.getCount10s());
        if (dto.isDirty("count5s")) cashBank.setCount5s(dto.getCount5s());
        if (dto.isDirty("count1s")) cashBank.setCount1s(dto.getCount1s());
        if (dto.isDirty("count1cs")) cashBank.setCount1cs(dto.getCount1cs());
        if (dto.isDirty("status")) cashBank.setStatus(dto.getStatus());
        if (dto.isDirty("deviceId")) cashBank.setDeviceId(dto.getDeviceId());
        if (dto.isDirty("parentCashBankId")) cashBank.setParentCashBankId(dto.getParentCashBankId());
        if (dto.isDirty("processId")) cashBank.setProcessId(dto.getProcessId());

        if (dto.isDirty("operationId")) {
            if (dto.getOperationId() == null) {
                cashBank.setOperation(null);
            } else {
                Operation operation = operationRepository.findById(dto.getOperationId())
                        .orElseThrow(() -> new EntityNotFoundException("Operation not found with id: " + dto.getOperationId()));
                cashBank.setOperation(operation);
            }
        }

        if (dto.isDirty("shiftId")) {
            if (dto.getShiftId() == null) {
                cashBank.setShift(null);
            } else {
                Shift shift = shiftRepository.findById(dto.getShiftId())
                        .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + dto.getShiftId()));
                cashBank.setShift(shift);
            }
        }

        if (dto.isDirty("gateId")) {
            if (dto.getGateId() == null) {
                cashBank.setGate(null);
            } else {
                Gate gate = gateRepository.findById(dto.getGateId())
                        .orElseThrow(() -> new EntityNotFoundException("Gate not found with id: " + dto.getGateId()));
                cashBank.setGate(gate);
            }
        }

        if (dto.isDirty("personStickerId")) {
            if (dto.getPersonStickerId() == null) {
                cashBank.setPerson(null);
            } else {
                cashBank.setPerson(resolvePerson(dto.getPersonStickerId()));
            }
        }

        String currentUser = "System";
        cashBank.setUpdatedBy(currentUser);

        CashBank saved = cashBankRepository.save(cashBank);
        saveCashBankHistory(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CashBank> searchCashBanks(
            UUID cashBankId, String bankName, BigDecimal startingAmount, BigDecimal officialCount,
            Integer count100s, Integer count50s, Integer count20s, Integer count10s,
            Integer count5s, Integer count1s, Integer count1cs, String status, UUID operationId,
            UUID shiftId, UUID gateId, String deviceId, UUID parentCashBankId,
            UUID processId, UUID personStickerId, String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) {
        Specification<CashBank> spec = CashBankSpecification.search(
                cashBankId, bankName, startingAmount, officialCount, count100s, count50s,
                count20s, count10s, count5s, count1s, count1cs, status, operationId, shiftId, gateId,
                deviceId, parentCashBankId, processId, personStickerId, createdBy, createdAt, updatedBy, updatedAt);
        return cashBankRepository.findAll(spec);
    }

    private void saveCashBankHistory(CashBank cashBank) {
        CashBankHistory history = new CashBankHistory();
        history.setCashBankId(cashBank.getCashBankId());
        history.setBankName(cashBank.getBankName());
        history.setStartingAmount(cashBank.getStartingAmount());
        history.setOfficialCount(cashBank.getOfficialCount());
        history.setCount100s(cashBank.getCount100s());
        history.setCount50s(cashBank.getCount50s());
        history.setCount20s(cashBank.getCount20s());
        history.setCount10s(cashBank.getCount10s());
        history.setCount5s(cashBank.getCount5s());
        history.setCount1s(cashBank.getCount1s());
        history.setCount1cs(cashBank.getCount1cs());
        history.setStatus(cashBank.getStatus());
        history.setOperation(cashBank.getOperation());
        history.setShift(cashBank.getShift());
        history.setGate(cashBank.getGate());
        history.setDeviceId(cashBank.getDeviceId());
        history.setParentCashBankId(cashBank.getParentCashBankId());
        history.setProcessId(cashBank.getProcessId());
        history.setCreatedBy(cashBank.getUpdatedBy() != null ? cashBank.getUpdatedBy() : cashBank.getCreatedBy());
        cashBankHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<CashBankHistory> getHistoryByShiftId(UUID shiftId) {
        return cashBankHistoryRepository.findByShift_ShiftId(shiftId);
    }

    @Transactional(readOnly = true)
    public List<CashBank> getCashBanksByIds(List<UUID> ids) {
        return cashBankRepository.findAllById(ids);
    }

    public List<CashBank> getCashBanks(UUID operationId, UUID shiftId) {
        if (operationId != null && shiftId != null) {
            return cashBankRepository.findByOperationOperationIdAndShiftShiftId(operationId, shiftId);
        } else if (shiftId != null) {
            return cashBankRepository.findByShiftShiftId(shiftId);
        } else if (operationId != null) {
            return cashBankRepository.findByOperationOperationId(operationId);
        }
        return cashBankRepository.findAll();
    }

    public Optional<CashBank> getCashBankById(UUID id) {
        return cashBankRepository.findById(id);
    }

    @Transactional
    public CashBank saveCashBank(CashBank cashBank) {
        return cashBankRepository.save(cashBank);
    }

    @Transactional
    public CashBank updateStatus(UUID id, String status) {
        CashBank bank = cashBankRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("CashBank not found: " + id));
        bank.setStatus(status);
        return cashBankRepository.save(bank);
    }

    @Transactional
    public CashBank reconcileBank(UUID id, BigDecimal officialCount) {
        CashBank bank = cashBankRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("CashBank not found: " + id));
        if (officialCount != null) {
            bank.setOfficialCount(officialCount);
        }
        bank.setStatus("RECONCILED");
        return cashBankRepository.save(bank);
    }

    @Transactional
    public List<CashBank> reconcileBanks(List<UUID> ids, BigDecimal officialCount) {
        List<CashBank> banks = cashBankRepository.findAllById(ids);
        for (CashBank bank : banks) {
            if (officialCount != null) {
                bank.setOfficialCount(officialCount);
            }
            bank.setStatus("RECONCILED");
        }
        return cashBankRepository.saveAll(banks);
    }
}
