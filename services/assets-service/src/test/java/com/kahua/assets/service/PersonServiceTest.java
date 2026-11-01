package com.kahua.assets.service;

import com.kahua.assets.dto.PersonDto;
import com.kahua.assets.dto.PersonBulkDto;
import com.kahua.assets.entity.Person;
import com.kahua.assets.repository.PersonRepository;
import com.kahua.assets.repository.PersonStickerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PersonServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private com.kahua.assets.repository.ShiftRepository shiftRepository;

    @Mock
    private com.kahua.assets.repository.GateRepository gateRepository;

    @Mock
    private PersonStickerRepository personStickerRepository;

    @InjectMocks
    private PersonService personService;

    @Test
    void testCreatePerson_withNullableNames() {
        when(personRepository.save(any(Person.class))).thenAnswer(i -> i.getArgument(0));

        // both names null
        PersonDto dto = new PersonDto();
        Person result = personService.createPerson(dto);
        assertTrue(result.getFirstName().matches("^Unknown [A-Z0-9]{4}$"));
        assertNull(result.getLastName());

        // Blank names
        dto.setFirstName("");
        dto.setLastName("   ");
        result = personService.createPerson(dto);
        assertTrue(result.getFirstName().matches("^Unknown [A-Z0-9]{4}$"));
        assertNull(result.getLastName());

        // Literal "Unknown"
        dto.setFirstName("Unknown");
        dto.setLastName(null);
        result = personService.createPerson(dto);
        assertTrue(result.getFirstName().matches("^Unknown [A-Z0-9]{4}$"));
        assertNull(result.getLastName());

        // firstName provided, lastName null/blank
        dto.setFirstName("  Jane Doe  ");
        dto.setLastName("");
        result = personService.createPerson(dto);
        assertEquals("Jane Doe", result.getFirstName());
        assertNull(result.getLastName());

        // firstName null/blank, lastName provided
        dto.setFirstName(null);
        dto.setLastName("  Smith  ");
        result = personService.createPerson(dto);
        assertEquals("Smith", result.getFirstName());
        assertNull(result.getLastName());

        // Both provided
        dto.setFirstName("  Jane  ");
        dto.setLastName("  Doe  ");
        result = personService.createPerson(dto);
        assertEquals("Jane", result.getFirstName());
        assertEquals("Doe", result.getLastName());
    }

    @Test
    void testBulkCreatePersons_withNullableNames() {
        when(personRepository.save(any(Person.class))).thenAnswer(i -> i.getArgument(0));

        PersonBulkDto dto1 = new PersonBulkDto();
        dto1.setFirstName(" ");
        dto1.setLastName(null);

        PersonBulkDto dto2 = new PersonBulkDto();
        dto2.setFirstName("Jane Doe");
        dto2.setLastName(null);

        List<Person> results = personService.bulkCreatePersons(List.of(dto1, dto2));

        assertEquals(2, results.size());
        assertTrue(results.get(0).getFirstName().matches("^Unknown [A-Z0-9]{4}$"));
        assertNull(results.get(0).getLastName());
        assertEquals("Jane Doe", results.get(1).getFirstName());
        assertNull(results.get(1).getLastName());
    }

    @Test
    void testBulkCreatePersons_withShiftAndGenerateSticker() {
        java.util.UUID shiftId = java.util.UUID.randomUUID();
        com.kahua.assets.entity.Shift mockShift = new com.kahua.assets.entity.Shift();
        mockShift.setShiftId(shiftId);
        mockShift.setName("Morning Shift");

        when(shiftRepository.findById(shiftId)).thenReturn(java.util.Optional.of(mockShift));
        when(personRepository.save(any(Person.class))).thenAnswer(i -> {
            Person p = i.getArgument(0);
            p.setPersonId(java.util.UUID.randomUUID());
            return p;
        });
        when(personStickerRepository.save(any(com.kahua.assets.entity.PersonSticker.class))).thenAnswer(i -> i.getArgument(0));

        PersonBulkDto dto = new PersonBulkDto();
        dto.setFirstName("Alice");
        dto.setLastName("Walker");
        dto.setShiftId(shiftId);
        dto.setGenerateSticker(true);

        List<Person> results = personService.bulkCreatePersons(List.of(dto));

        assertEquals(1, results.size());
        assertEquals("Alice", results.get(0).getFirstName());
        assertEquals("Walker", results.get(0).getLastName());
        assertEquals(mockShift, results.get(0).getShift());

        org.mockito.Mockito.verify(personStickerRepository, org.mockito.Mockito.times(1))
                .save(org.mockito.ArgumentMatchers.argThat(s ->
                        s.getPerson().equals(results.get(0)) &&
                        s.getShift().equals(mockShift)
                ));
    }
}
