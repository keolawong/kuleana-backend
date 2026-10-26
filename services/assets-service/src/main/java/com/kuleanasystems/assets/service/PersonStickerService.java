package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.dto.PersonStickerDto;
import com.kuleanasystems.assets.dto.PersonStickerPatchDto;
import com.kuleanasystems.assets.entity.Gate;
import com.kuleanasystems.assets.entity.Person;
import com.kuleanasystems.assets.entity.PersonSticker;
import com.kuleanasystems.assets.entity.Shift;
import com.kuleanasystems.assets.repository.GateRepository;
import com.kuleanasystems.assets.repository.PersonRepository;
import com.kuleanasystems.assets.repository.PersonStickerRepository;
import com.kuleanasystems.assets.repository.PersonStickerSpecification;
import com.kuleanasystems.assets.repository.ShiftRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonStickerService {

    private final PersonStickerRepository personStickerRepository;
    private final PersonRepository personRepository;
    private final ShiftRepository shiftRepository;
    private final GateRepository gateRepository;

    @Transactional
    public PersonSticker createPersonSticker(PersonStickerDto dto) {
        Person person = null;
        if (dto.getPersonId() != null) {
            person = personRepository.findById(dto.getPersonId())
                    .orElseThrow(() -> new EntityNotFoundException("Person not found with id: " + dto.getPersonId()));
        }

        Shift shift = null;
        if (dto.getShiftId() != null) {
            shift = shiftRepository.findById(dto.getShiftId())
                    .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + dto.getShiftId()));
        }

        Gate gate = null;
        if (dto.getGateId() != null) {
            gate = gateRepository.findById(dto.getGateId())
                    .orElseThrow(() -> new EntityNotFoundException("Gate not found with id: " + dto.getGateId()));
        }

        PersonSticker personSticker = new PersonSticker();
        personSticker.setPerson(person);
        personSticker.setShift(shift);
        personSticker.setGate(gate);

        if (person != null && shift != null) {
            PersonSticker existing = personStickerRepository.findFirstByPerson_PersonIdAndShift_ShiftId(person.getPersonId(), shift.getShiftId());
            if (existing != null) {
                if (gate != null && (existing.getGate() == null || !gate.getGateId().equals(existing.getGate().getGateId()))) {
                    existing.setGate(gate);
                    existing.setUpdatedBy("System");
                    return personStickerRepository.save(existing);
                }
                return existing;
            }
        }

        String currentUser = "System";
        personSticker.setCreatedBy(currentUser);

        return personStickerRepository.save(personSticker);
    }

    @Transactional
    public PersonSticker updatePersonSticker(UUID personStickerId, PersonStickerPatchDto dto) {
        PersonSticker personSticker = personStickerRepository.findById(personStickerId)
                .orElseThrow(() -> new EntityNotFoundException("PersonSticker not found with id: " + personStickerId));

        if (dto.isDirty("personId")) {
            if (dto.getPersonId() == null) {
                personSticker.setPerson(null);
            } else {
                Person person = personRepository.findById(dto.getPersonId())
                        .orElseThrow(() -> new EntityNotFoundException("Person not found with id: " + dto.getPersonId()));
                personSticker.setPerson(person);
            }
        }
        if (dto.isDirty("shiftId")) {
            if (dto.getShiftId() == null) {
                personSticker.setShift(null);
            } else {
                Shift shift = shiftRepository.findById(dto.getShiftId())
                        .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + dto.getShiftId()));
                personSticker.setShift(shift);
            }
        }
        if (dto.isDirty("gateId")) {
            if (dto.getGateId() == null) {
                personSticker.setGate(null);
            } else {
                Gate gate = gateRepository.findById(dto.getGateId())
                        .orElseThrow(() -> new EntityNotFoundException("Gate not found with id: " + dto.getGateId()));
                personSticker.setGate(gate);
            }
        }

        if (personSticker.getPerson() != null && personSticker.getShift() != null) {
            boolean conflict = personStickerRepository.existsByPerson_PersonIdAndShift_ShiftIdAndPersonStickerIdNot(
                    personSticker.getPerson().getPersonId(), 
                    personSticker.getShift().getShiftId(), 
                    personStickerId
            );
            if (conflict) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seller can only be assigned to 1 gate/draft per shift.");
            }
        }

        String currentUser = "System";
        personSticker.setUpdatedBy(currentUser);

        return personStickerRepository.save(personSticker);
    }

    @Transactional(readOnly = true)
    public List<PersonSticker> searchPersonStickers(
            UUID personStickerId, UUID personId, UUID shiftId, UUID gateId,
            String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) {
        Specification<PersonSticker> spec = PersonStickerSpecification.search(
                personStickerId, personId, shiftId, gateId,
                createdBy, createdAt, updatedBy, updatedAt);
        return personStickerRepository.findAll(spec);
    }
}
