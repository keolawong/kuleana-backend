package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.dto.PersonBulkDto;
import com.kuleanasystems.assets.dto.PersonDto;
import com.kuleanasystems.assets.dto.PersonPatchDto;
import com.kuleanasystems.assets.entity.Gate;
import com.kuleanasystems.assets.entity.Person;
import com.kuleanasystems.assets.entity.Shift;
import com.kuleanasystems.assets.repository.GateRepository;
import com.kuleanasystems.assets.repository.PersonRepository;
import com.kuleanasystems.assets.repository.PersonSpecification;
import com.kuleanasystems.assets.repository.ShiftRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final ShiftRepository shiftRepository;
    private final GateRepository gateRepository;
    private final com.kuleanasystems.assets.repository.PersonStickerRepository personStickerRepository;

    @Transactional
    public Person createPerson(PersonDto dto) {
        Person person = new Person();
        applyNormalizedNames(person, dto.getFirstName(), dto.getLastName());
        person.setNotes(dto.getNotes());
        person.setExternalId(dto.getExternalId());

        String currentUser = "System";
        person.setCreatedBy(currentUser);

        return personRepository.save(person);
    }

    @Transactional
    public List<Person> bulkCreatePersons(List<PersonBulkDto> dtos) {
        List<Person> createdPersons = new ArrayList<>();
        String currentUser = "System";

        for (PersonBulkDto dto : dtos) {
            Person person = new Person();
            applyNormalizedNames(person, dto.getFirstName(), dto.getLastName());
            person.setNotes(dto.getNotes());
            person.setExternalId(dto.getExternalId());
            if (dto.getOrganization() != null) {
                person.setOrganization(dto.getOrganization());
            }
            if (dto.getJobRole() != null) {
                person.setJobRole(dto.getJobRole());
            }
            if (dto.getIsActive() != null) {
                person.setIsActive(dto.getIsActive());
            }
            Shift shift = null;
            if (dto.getShiftId() != null) {
                shift = shiftRepository.findById(dto.getShiftId())
                        .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + dto.getShiftId()));
                person.setShift(shift);
            }
            Gate gate = null;
            if (dto.getGateId() != null) {
                gate = gateRepository.findById(dto.getGateId())
                        .orElseThrow(() -> new EntityNotFoundException("Gate not found with id: " + dto.getGateId()));
                person.setGate(gate);
            }
            person.setCreatedBy(currentUser);
            person = personRepository.save(person);
            createdPersons.add(person);

            if (dto.isGenerateSticker()) {
                String role = person.getJobRole();
                if (role == null || !"Ticket Scanner".equalsIgnoreCase(role.trim())) {
                    com.kuleanasystems.assets.entity.PersonSticker sticker = null;
                    if (shift != null) {
                        sticker = personStickerRepository.findFirstByPerson_PersonIdAndShift_ShiftId(person.getPersonId(), shift.getShiftId());
                    }
                    if (sticker == null) {
                        sticker = new com.kuleanasystems.assets.entity.PersonSticker();
                        sticker.setPerson(person);
                        sticker.setShift(shift);
                        sticker.setGate(gate);
                        sticker.setCreatedBy(currentUser);
                        personStickerRepository.save(sticker);
                    } else if (gate != null && (sticker.getGate() == null || !gate.getGateId().equals(sticker.getGate().getGateId()))) {
                        sticker.setGate(gate);
                        sticker.setUpdatedBy(currentUser);
                        personStickerRepository.save(sticker);
                    }
                }
            }
        }
        return createdPersons;
    }

    @Transactional
    public Person updatePerson(UUID personId, PersonPatchDto dto) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new EntityNotFoundException("Person not found with id: " + personId));

        if (dto.isDirty("firstName")) {
            person.setFirstName(dto.getFirstName());
        }
        if (dto.isDirty("lastName")) {
            person.setLastName(dto.getLastName());
        }
        if (dto.isDirty("notes")) {
            person.setNotes(dto.getNotes());
        }
        if (dto.isDirty("externalId")) {
            person.setExternalId(dto.getExternalId());
        }
        if (dto.isDirty("organization")) {
            person.setOrganization(dto.getOrganization());
        }
        if (dto.isDirty("jobRole")) {
            person.setJobRole(dto.getJobRole());
        }
        if (dto.isDirty("isActive")) {
            person.setIsActive(dto.getIsActive());
        }
        if (dto.isDirty("shiftId")) {
            if (dto.getShiftId() == null) {
                person.setShift(null);
            } else {
                Shift shift = shiftRepository.findById(dto.getShiftId())
                        .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + dto.getShiftId()));
                person.setShift(shift);
            }
        }
        if (dto.isDirty("gateId")) {
            if (dto.getGateId() == null) {
                person.setGate(null);
            } else {
                Gate gate = gateRepository.findById(dto.getGateId())
                        .orElseThrow(() -> new EntityNotFoundException("Gate not found with id: " + dto.getGateId()));
                person.setGate(gate);
            }
        }

        String currentUser = "System";
        person.setUpdatedBy(currentUser);

        return personRepository.save(person);
    }

    @Transactional(readOnly = true)
    public List<Person> searchPersons(
            UUID personId, String firstName, String lastName, String notes, String externalId,
            UUID shiftId, UUID gateId, String organization, String jobRole, Boolean isActive,
            String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) {
        Specification<Person> spec = PersonSpecification.search(
                personId, firstName, lastName, notes, externalId,
                shiftId, gateId, organization, jobRole, isActive,
                createdBy, createdAt, updatedBy, updatedAt);
        return personRepository.findAll(spec);
    }

    private static final String UNIQUE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateUnknownName() {
        StringBuilder sb = new StringBuilder("Unknown ");
        for (int i = 0; i < 4; i++) {
            sb.append(UNIQUE_CHARS.charAt(RANDOM.nextInt(UNIQUE_CHARS.length())));
        }
        return sb.toString();
    }

    private void applyNormalizedNames(Person person, String firstName, String lastName) {
        boolean hasFirst = firstName != null && !firstName.isBlank();
        boolean hasLast = lastName != null && !lastName.isBlank();

        if (hasFirst && hasLast) {
            person.setFirstName(firstName.trim());
            person.setLastName(lastName.trim());
        } else if (hasFirst) {
            String trimmed = firstName.trim();
            if (trimmed.equalsIgnoreCase("Unknown")) {
                person.setFirstName(generateUnknownName());
            } else {
                person.setFirstName(trimmed);
            }
            person.setLastName(null);
        } else if (hasLast) {
            String trimmed = lastName.trim();
            if (trimmed.equalsIgnoreCase("Unknown")) {
                person.setFirstName(generateUnknownName());
            } else {
                person.setFirstName(trimmed);
            }
            person.setLastName(null);
        } else {
            person.setFirstName(generateUnknownName());
            person.setLastName(null);
        }
    }
}
