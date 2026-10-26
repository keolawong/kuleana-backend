package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.dto.PersonDto;
import com.kuleanasystems.assets.dto.PersonPatchDto;
import com.kuleanasystems.assets.entity.Person;
import com.kuleanasystems.assets.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @PostMapping
    public ResponseEntity<Person> createPerson(@Valid @RequestBody PersonDto personDto) {
        Person createdPerson = personService.createPerson(personDto);
        return ResponseEntity.created(URI.create("/api/persons/" + createdPerson.getPersonId())).body(createdPerson);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<Person>> bulkCreatePersons(@Valid @RequestBody List<com.kuleanasystems.assets.dto.PersonBulkDto> dtos) {
        List<Person> createdPersons = personService.bulkCreatePersons(dtos);
        return ResponseEntity.ok(createdPersons);
    }

    @PatchMapping("/{personId}")
    public ResponseEntity<Person> updatePerson(@PathVariable UUID personId, @RequestBody PersonPatchDto personPatchDto) {
        Person updatedPerson = personService.updatePerson(personId, personPatchDto);
        return ResponseEntity.ok(updatedPerson);
    }

    @GetMapping
    public ResponseEntity<List<Person>> getPersons(
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

        List<Person> persons = personService.searchPersons(
                personId, firstName, lastName, notes, externalId,
                shiftId, gateId, organization, jobRole, isActive,
                createdBy, createdAt, updatedBy, updatedAt);

        if (persons.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(persons);
    }
}
