package com.kahua.assets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kahua.assets.dto.TicketBookDto;
import com.kahua.assets.dto.TicketBookPatchDto;
import com.kahua.assets.entity.TicketBook;
import com.kahua.assets.entity.TicketBookHistory;
import com.kahua.assets.service.TicketBookService;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TicketBookControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TicketBookService ticketBookService;

    @InjectMocks
    private TicketBookController ticketBookController;

    private ObjectMapper objectMapper;

    private TicketBook mockBook;
    private UUID mockId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ticketBookController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockId = UUID.randomUUID();
        mockBook = new TicketBook();
        mockBook.setTicketBookId(mockId);
        mockBook.setScanCode("TB-123");
    }

    @Test
    void createTicketBook_shouldReturn201() throws Exception {
        TicketBookDto dto = new TicketBookDto();
        dto.setScanCode("TB-123");
        dto.setStartNum(1);
        dto.setEndNum(100);

        when(ticketBookService.createTicketBook(any(TicketBookDto.class))).thenReturn(mockBook);

        mockMvc.perform(post("/api/ticket-books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/ticket-books/" + mockId))
                .andExpect(jsonPath("$.scanCode").value("TB-123"));
    }

    @Test
    void createTicketBooksBulk_shouldReturn201() throws Exception {
        TicketBookDto dto = new TicketBookDto();
        dto.setScanCode("TB-123");
        dto.setStartNum(1);
        dto.setEndNum(100);

        when(ticketBookService.createTicketBooks(anyList())).thenReturn(List.of(mockBook));

        mockMvc.perform(post("/api/ticket-books/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(dto))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].scanCode").value("TB-123"));
    }

    @Test
    void splitTicketBooks_shouldReturn200() throws Exception {
        when(ticketBookService.splitTicketBooks(anyList())).thenReturn(List.of(mockBook));

        mockMvc.perform(post("/api/ticket-books/bulk-split")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(mockId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scanCode").value("TB-123"));
    }

    @Test
    void getMaxTicketsSold_shouldReturn200() throws Exception {
        UUID opId = UUID.randomUUID();
        when(ticketBookService.getMaxTicketsSoldMetrics(eq(opId), eq("Shift1"), eq("Gate1"))).thenReturn(50);

        mockMvc.perform(get("/api/ticket-books/max-sold/{excludeOperationId}/{shiftName}/{gateName}", opId, "Shift1", "Gate1"))
                .andExpect(status().isOk())
                .andExpect(content().string("50"));
    }

    @Test
    void getTicketBooks_withShiftId_shouldReturnMergedHistory() throws Exception {
        UUID shiftId = UUID.randomUUID();

        TicketBookHistory history = new TicketBookHistory();
        history.setTicketBookId(mockId);
        history.setScanCode("TB-History");
        history.setStatus("Returned");
        history.setCreatedAt(java.time.Instant.now());

        when(ticketBookService.searchTicketBooks(any(), any(), any(), any(), any(), any(), any(), any(), eq(shiftId), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of()); // No active books

        when(ticketBookService.getHistoryByShiftId(shiftId)).thenReturn(List.of(history));
        when(ticketBookService.getTicketBooksByIds(anyList())).thenReturn(List.of());

        mockMvc.perform(get("/api/ticket-books")
                .param("shiftId", shiftId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scanCode").value("TB-History"));
    }
}
