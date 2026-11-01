package com.kahua.assets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kahua.assets.dto.PersonDto;
import com.kahua.assets.dto.PersonPatchDto;
import com.kahua.assets.entity.Person;
import com.kahua.assets.service.PersonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PersonControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PersonService personService;

    @InjectMocks
    private PersonController personController;

    private ObjectMapper objectMapper;

    private Person mockPerson;
    private UUID mockId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(personController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockId = UUID.randomUUID();
        mockPerson = new Person();
        mockPerson.setPersonId(mockId);
        mockPerson.setFirstName("John");
        mockPerson.setLastName("Doe");
    }

    @Test
    void createPerson_shouldReturn201() throws Exception {
        PersonDto dto = new PersonDto();
        dto.setFirstName("John");
        dto.setLastName("Doe");

        when(personService.createPerson(any(PersonDto.class))).thenReturn(mockPerson);

        mockMvc.perform(post("/api/persons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/persons/" + mockId))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void updatePerson_shouldReturn200() throws Exception {
        PersonPatchDto patch = new PersonPatchDto();
        
        when(personService.updatePerson(eq(mockId), any(PersonPatchDto.class))).thenReturn(mockPerson);

        mockMvc.perform(patch("/api/persons/{id}", mockId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void getPeople_shouldReturn200() throws Exception {
        when(personService.searchPersons(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockPerson));

        mockMvc.perform(get("/api/persons")
                .param("firstName", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("John"));
    }

    @Test
    void getPeople_shouldReturn204IfEmpty() throws Exception {
        when(personService.searchPersons(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/persons"))
                .andExpect(status().isNoContent());
    }
}
