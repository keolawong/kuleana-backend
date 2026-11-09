package com.kahua.assets.controller;

import com.kahua.assets.dto.PersonBulkDto;
import com.kahua.assets.dto.PersonDto;
import com.kahua.assets.dto.PersonPatchDto;
import com.kahua.assets.entity.Person;
import com.kahua.assets.service.PersonService;
import com.kuleanasystems.security.TenantSecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

    private static final UUID BALLOON_FIESTA_ORG_ID = UUID.fromString("b0000000-0000-0000-0000-000000000002");

    private final PersonService personService;

    @PostMapping
    public ResponseEntity<Person> createPerson(
            @Valid @RequestBody PersonDto personDto,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String targetOrgStr = personDto.getOrgId() != null ? personDto.getOrgId().toString() : orgIdHeader;
        UUID effectiveOrgId = TenantSecurityUtil.resolveEffectiveOrgId(targetOrgStr, auth);
        personDto.setOrgId(effectiveOrgId != null ? effectiveOrgId : BALLOON_FIESTA_ORG_ID);

        Person createdPerson = personService.createPerson(personDto);
        return ResponseEntity.created(URI.create("/api/persons/" + createdPerson.getPersonId())).body(createdPerson);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<Person>> bulkCreatePersons(
            @Valid @RequestBody List<PersonBulkDto> dtos,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID effectiveOrgId = TenantSecurityUtil.resolveEffectiveOrgId(orgIdHeader, auth);
        UUID targetOrgId = effectiveOrgId != null ? effectiveOrgId : BALLOON_FIESTA_ORG_ID;

        for (PersonBulkDto dto : dtos) {
            if (dto.getOrgId() == null) {
                dto.setOrgId(targetOrgId);
            } else if (!TenantSecurityUtil.isMemberOf(auth, dto.getOrgId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to org: " + dto.getOrgId());
            }
        }

        List<Person> createdPersons = personService.bulkCreatePersons(dtos);
        return ResponseEntity.ok(createdPersons);
    }

    @PatchMapping("/{personId}")
    public ResponseEntity<Person> updatePerson(
            @PathVariable UUID personId,
            @RequestBody PersonPatchDto personPatchDto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Person existing = personService.getPersonById(personId);
        if (existing.getOrgId() != null && !TenantSecurityUtil.isMemberOf(auth, existing.getOrgId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to person in another organization");
        }
        Person updatedPerson = personService.updatePerson(personId, personPatchDto);
        return ResponseEntity.ok(updatedPerson);
    }

    @GetMapping("/{personId}")
    public ResponseEntity<Person> getPersonById(@PathVariable UUID personId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Person person = personService.getPersonById(personId);
        if (person.getOrgId() != null && !TenantSecurityUtil.isMemberOf(auth, person.getOrgId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to person in another organization");
        }
        return ResponseEntity.ok(person);
    }

    @GetMapping
    public ResponseEntity<List<Person>> getPersons(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader,
            @RequestParam(required = false) UUID orgId,
            @RequestParam(required = false) UUID personId,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String externalId,
            @RequestParam(required = false) UUID shiftId,
            @RequestParam(required = false) UUID gateId,
            @RequestParam(required = false) String organization,
            @RequestParam(required = false) String jobRole,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Instant createdAt,
            @RequestParam(required = false) String updatedBy,
            @RequestParam(required = false) Instant updatedAt) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String targetOrgStr = (orgId != null) ? orgId.toString() : orgIdHeader;
        UUID effectiveOrgId = TenantSecurityUtil.resolveEffectiveOrgId(targetOrgStr, auth);

        List<Person> persons = personService.searchPersons(
                personId, effectiveOrgId, firstName, lastName, notes, externalId,
                shiftId, gateId, organization, jobRole, isActive,
                createdBy, createdAt, updatedBy, updatedAt);

        if (persons.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(persons);
    }
}
