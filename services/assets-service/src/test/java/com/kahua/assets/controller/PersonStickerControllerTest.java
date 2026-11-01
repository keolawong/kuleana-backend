package com.kahua.assets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kahua.assets.dto.PersonStickerDto;
import com.kahua.assets.dto.PersonStickerPatchDto;
import com.kahua.assets.entity.PersonSticker;
import com.kahua.assets.service.PersonStickerService;
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
class PersonStickerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PersonStickerService personStickerService;

    @InjectMocks
    private PersonStickerController personStickerController;

    private ObjectMapper objectMapper;

    private PersonSticker mockSticker;
    private UUID mockId;
    private UUID mockPersonId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(personStickerController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockId = UUID.randomUUID();
        mockPersonId = UUID.randomUUID();
        mockSticker = new PersonSticker();
        mockSticker.setPersonStickerId(mockId);
    }

    @Test
    void createPersonSticker_shouldReturn201() throws Exception {
        PersonStickerDto dto = new PersonStickerDto();
        dto.setPersonId(mockPersonId);

        when(personStickerService.createPersonSticker(any(PersonStickerDto.class))).thenReturn(mockSticker);

        mockMvc.perform(post("/api/person-stickers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/person-stickers/" + mockId));
    }

    @Test
    void updatePersonSticker_shouldReturn200() throws Exception {
        PersonStickerPatchDto patch = new PersonStickerPatchDto();
        
        when(personStickerService.updatePersonSticker(eq(mockId), any(PersonStickerPatchDto.class))).thenReturn(mockSticker);

        mockMvc.perform(patch("/api/person-stickers/{id}", mockId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk());
    }

    @Test
    void getPersonStickers_shouldReturn200() throws Exception {
        when(personStickerService.searchPersonStickers(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockSticker));

        mockMvc.perform(get("/api/person-stickers")
                .param("personId", mockPersonId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void getPersonStickers_shouldReturn204IfEmpty() throws Exception {
        when(personStickerService.searchPersonStickers(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/person-stickers"))
                .andExpect(status().isNoContent());
    }
}
