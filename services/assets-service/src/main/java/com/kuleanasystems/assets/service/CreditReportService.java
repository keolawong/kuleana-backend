package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.dto.CreditReportDto;
import com.kuleanasystems.assets.dto.CreditReportPatchDto;
import com.kuleanasystems.assets.entity.CreditReport;
import com.kuleanasystems.assets.entity.Gate;
import com.kuleanasystems.assets.entity.Operation;
import com.kuleanasystems.assets.entity.Shift;
import com.kuleanasystems.assets.entity.Person;
import com.kuleanasystems.assets.entity.PersonSticker;
import com.kuleanasystems.assets.entity.CreditReportHistory;
import com.kuleanasystems.assets.repository.CreditReportHistoryRepository;
import com.kuleanasystems.assets.repository.CreditReportRepository;
import com.kuleanasystems.assets.repository.CreditReportSpecification;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditReportService {

    private final CreditReportRepository creditReportRepository;
    private final CreditReportHistoryRepository creditReportHistoryRepository;
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
    public CreditReport createCreditReport(CreditReportDto dto) {
        CreditReport creditReport = new CreditReport();
        creditReport.setTotalAmount(dto.getTotalAmount());
        creditReport.setDeviceId(dto.getDeviceId());
        creditReport.setProcessId(dto.getProcessId());

        if (dto.getOperationId() != null) {
            Operation operation = operationRepository.findById(dto.getOperationId())
                    .orElseThrow(() -> new EntityNotFoundException("Operation not found with id: " + dto.getOperationId()));
            creditReport.setOperation(operation);
        }
        if (dto.getShiftId() != null) {
            Shift shift = shiftRepository.findById(dto.getShiftId())
                    .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + dto.getShiftId()));
            creditReport.setShift(shift);
        }
        if (dto.getGateId() != null) {
            Gate gate = gateRepository.findById(dto.getGateId())
                    .orElseThrow(() -> new EntityNotFoundException("Gate not found with id: " + dto.getGateId()));
            creditReport.setGate(gate);
        }
        UUID targetPersonId = dto.getPersonStickerId();
        if (targetPersonId != null) {
            creditReport.setPerson(resolvePerson(targetPersonId));
        }

        String currentUser = "System";
        creditReport.setCreatedBy(currentUser);

        CreditReport saved = creditReportRepository.save(creditReport);
        saveCreditReportHistory(saved);
        return saved;
    }

    @Transactional
    public CreditReport updateCreditReport(UUID creditReportId, CreditReportPatchDto dto) {
        CreditReport creditReport = creditReportRepository.findById(creditReportId)
                .orElseThrow(() -> new EntityNotFoundException("CreditReport not found with id: " + creditReportId));

        if (dto.isDirty("totalAmount")) creditReport.setTotalAmount(dto.getTotalAmount());
        if (dto.isDirty("deviceId")) creditReport.setDeviceId(dto.getDeviceId());
        if (dto.isDirty("processId")) creditReport.setProcessId(dto.getProcessId());

        if (dto.isDirty("operationId")) {
            if (dto.getOperationId() == null) {
                creditReport.setOperation(null);
            } else {
                Operation operation = operationRepository.findById(dto.getOperationId())
                        .orElseThrow(() -> new EntityNotFoundException("Operation not found with id: " + dto.getOperationId()));
                creditReport.setOperation(operation);
            }
        }
        if (dto.isDirty("shiftId")) {
            if (dto.getShiftId() == null) {
                creditReport.setShift(null);
            } else {
                Shift shift = shiftRepository.findById(dto.getShiftId())
                        .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + dto.getShiftId()));
                creditReport.setShift(shift);
            }
        }
        if (dto.isDirty("gateId")) {
            if (dto.getGateId() == null) {
                creditReport.setGate(null);
            } else {
                Gate gate = gateRepository.findById(dto.getGateId())
                        .orElseThrow(() -> new EntityNotFoundException("Gate not found with id: " + dto.getGateId()));
                creditReport.setGate(gate);
            }
        }
        if (dto.isDirty("personStickerId")) {
            if (dto.getPersonStickerId() == null) {
                creditReport.setPerson(null);
            } else {
                creditReport.setPerson(resolvePerson(dto.getPersonStickerId()));
            }
        }

        String currentUser = "System";
        creditReport.setUpdatedBy(currentUser);

        CreditReport saved = creditReportRepository.save(creditReport);
        saveCreditReportHistory(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CreditReport> searchCreditReports(
            UUID creditReportId, BigDecimal totalAmount, UUID operationId, UUID shiftId,
            UUID gateId, String deviceId, UUID processId, UUID personStickerId, String createdBy,
            Instant createdAt, String updatedBy, Instant updatedAt) {
        Specification<CreditReport> spec = CreditReportSpecification.search(
                creditReportId, totalAmount, operationId, shiftId, gateId,
                deviceId, processId, personStickerId, createdBy, createdAt, updatedBy, updatedAt);
        return creditReportRepository.findAll(spec);
    }

    private void saveCreditReportHistory(CreditReport creditReport) {
        CreditReportHistory history = new CreditReportHistory();
        history.setCreditReportId(creditReport.getCreditReportId());
        history.setTotalAmount(creditReport.getTotalAmount());
        history.setOperation(creditReport.getOperation());
        history.setShift(creditReport.getShift());
        history.setGate(creditReport.getGate());
        history.setDeviceId(creditReport.getDeviceId());
        history.setProcessId(creditReport.getProcessId());
        history.setCreatedBy(creditReport.getUpdatedBy() != null ? creditReport.getUpdatedBy() : creditReport.getCreatedBy());
        creditReportHistoryRepository.save(history);
    }
}