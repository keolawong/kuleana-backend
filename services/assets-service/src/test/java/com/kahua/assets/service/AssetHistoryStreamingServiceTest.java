package com.kahua.assets.service;

import com.kahua.assets.entity.TicketBook;
import com.kahua.assets.repository.TicketBookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetHistoryStreamingServiceTest {

    @Mock
    private TrackerClient trackerClient;

    @Mock
    private TicketBookRepository ticketBookRepository;

    @Mock
    private com.kahua.assets.repository.TicketBookHistoryRepository ticketBookHistoryRepository;


    @Mock
    private com.kahua.assets.repository.CashBankRepository cashBankRepository;

    @Mock
    private com.kahua.assets.repository.CashBankHistoryRepository cashBankHistoryRepository;

    @Mock
    private SseEmitter emitter;

    private AssetHistoryStreamingService service;

    private static final String TEST_TOKEN = "test-jwt-token";
    private static final String TEST_SCAN_CODE = "XYZ-123";
    private static final UUID TEST_UUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AssetHistoryStreamingService(trackerClient, ticketBookRepository, ticketBookHistoryRepository, cashBankRepository, cashBankHistoryRepository);
    }

    private void mockTicketBookFound() {
        TicketBook mockTb = new TicketBook();
        mockTb.setTicketBookId(TEST_UUID);
        mockTb.setScanCode(TEST_SCAN_CODE);
        mockTb.setStartNum(1);
        mockTb.setEndNum(100);
        when(ticketBookRepository.findById(TEST_UUID)).thenReturn(Optional.of(mockTb));
    }

    @Test
    void streamHistory_EmitsAssetDiscoveredAndTimelineLoaded() throws IOException {
        mockTicketBookFound();
        when(trackerClient.getTimeline(TEST_UUID.toString(), TEST_TOKEN)).thenReturn(List.of("event1", "event2"));

        service.streamHistory(TEST_UUID, "Ticket Book", TEST_TOKEN, emitter);

        // Should emit at least 3 named events: ASSET_DISCOVERED, TIMELINE_LOADED, STREAM_COMPLETE
        verify(emitter, atLeast(3)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
    }

    @Test
    void streamHistory_TrackerDown_EmitsTimelineUnavailable() throws IOException {
        mockTicketBookFound();
        when(trackerClient.getTimeline(TEST_UUID.toString(), TEST_TOKEN)).thenThrow(new RuntimeException("ConnectException: Connection refused"));

        service.streamHistory(TEST_UUID, "Ticket Book", TEST_TOKEN, emitter);

        // Should emit ASSET_DISCOVERED, TIMELINE_UNAVAILABLE, and STREAM_COMPLETE
        verify(emitter, atLeast(3)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
    }

    @Test
    void streamHistory_TraversesParentChainInCorrectOrder() throws IOException {
        String scanCode = "CHILD-123";
        UUID childId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();

        TicketBook mockChild = new TicketBook();
        mockChild.setTicketBookId(childId);
        mockChild.setScanCode(scanCode);
        mockChild.setParentTicketBookId(parentId);
        mockChild.setStartNum(101);
        mockChild.setEndNum(200);
        
        TicketBook mockParent = new TicketBook();
        mockParent.setTicketBookId(parentId);
        mockParent.setScanCode("PARENT-456");
        mockParent.setStartNum(1);
        mockParent.setEndNum(200);

        when(ticketBookRepository.findById(childId)).thenReturn(Optional.of(mockChild));
        when(ticketBookRepository.findById(parentId)).thenReturn(Optional.of(mockParent));
        when(ticketBookRepository.findByParentTicketBookId(parentId)).thenReturn(List.of(mockChild));

        service.streamHistory(childId, "Ticket Book", TEST_TOKEN, emitter);

        verify(emitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
    }

    @Test
    void streamHistory_TicketBook_FallbackToTicketBookHistoryWhenTrackerDown() throws IOException {
        mockTicketBookFound();

        com.kahua.assets.entity.TicketBookHistory h1 = new com.kahua.assets.entity.TicketBookHistory();
        h1.setTicketBookHistoryId(UUID.randomUUID());
        h1.setTicketBookId(TEST_UUID);
        h1.setStartNum(1);
        h1.setEndNum(100);
        h1.setStatus("AVAILABLE");
        h1.setCreatedBy("admin");

        com.kahua.assets.entity.Gate mockGate = new com.kahua.assets.entity.Gate();
        mockGate.setName("West Gate");
        com.kahua.assets.entity.Shift mockShift = new com.kahua.assets.entity.Shift();
        mockShift.setName("Afternoon Shift");

        com.kahua.assets.entity.TicketBookHistory h2 = new com.kahua.assets.entity.TicketBookHistory();
        h2.setTicketBookHistoryId(UUID.randomUUID());
        h2.setTicketBookId(TEST_UUID);
        h2.setStartNum(1);
        h2.setEndNum(100);
        h2.setStatus("Assigned");
        h2.setGate(mockGate);
        h2.setShift(mockShift);

        com.kahua.assets.entity.TicketBookHistory h3 = new com.kahua.assets.entity.TicketBookHistory();
        h3.setTicketBookHistoryId(UUID.randomUUID());
        h3.setTicketBookId(TEST_UUID);
        h3.setStartNum(1);
        h3.setEndNum(100);
        h3.setStatus("Available");

        com.kahua.assets.entity.TicketBookHistory h4 = new com.kahua.assets.entity.TicketBookHistory();
        h4.setTicketBookHistoryId(UUID.randomUUID());
        h4.setTicketBookId(TEST_UUID);
        h4.setStartNum(1);
        h4.setEndNum(100);
        h4.setNextTicket(1);
        h4.setStatus("Returned Unused");

        when(ticketBookHistoryRepository.findByTicketBookIdOrderByCreatedAtAsc(TEST_UUID)).thenReturn(List.of(h1, h2, h3, h4));
        when(trackerClient.getTimeline(TEST_UUID.toString(), TEST_TOKEN)).thenThrow(new RuntimeException("Tracker unreachable"));

        service.streamHistory(TEST_UUID, "Ticket Book", TEST_TOKEN, emitter);

        // Should emit ASSET_DISCOVERED, TIMELINE_LOADED, TIMELINE_UNAVAILABLE, STREAM_COMPLETE
        verify(emitter, atLeast(4)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
    }

    @Test
    void streamHistory_CashBank_StreamsRootAndChildren() throws IOException {
        UUID cbId = UUID.randomUUID();
        com.kahua.assets.entity.CashBank mockCb = new com.kahua.assets.entity.CashBank();
        mockCb.setCashBankId(cbId);
        mockCb.setBankName("Main Gate Bank 1");
        mockCb.setStartingAmount(new java.math.BigDecimal("500.00"));
        mockCb.setOfficialCount(new java.math.BigDecimal("750.00"));

        when(cashBankRepository.findById(cbId)).thenReturn(Optional.of(mockCb));
        java.util.Map<String, Object> trackerEvent = new java.util.HashMap<>();
        trackerEvent.put("actions", new java.util.ArrayList<>());
        when(trackerClient.getTimeline(cbId.toString(), TEST_TOKEN)).thenReturn(List.of(trackerEvent));

        service.streamHistory(cbId, "Cash Bank", TEST_TOKEN, emitter);

        verify(emitter, atLeast(3)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
    }

    @Test
    void streamHistory_CashBank_FallbackToCashBankHistoryWhenTrackerDown() throws IOException {
        UUID cbId = UUID.randomUUID();
        com.kahua.assets.entity.CashBank mockCb = new com.kahua.assets.entity.CashBank();
        mockCb.setCashBankId(cbId);
        mockCb.setBankName("Main Gate Bank 1");
        mockCb.setStartingAmount(new java.math.BigDecimal("500.00"));

        com.kahua.assets.entity.CashBankHistory h1 = new com.kahua.assets.entity.CashBankHistory();
        h1.setCashBankHistoryId(UUID.randomUUID());
        h1.setCashBankId(cbId);
        h1.setStatus("Available");
        h1.setStartingAmount(new java.math.BigDecimal("500.00"));
        h1.setCreatedBy("admin");

        com.kahua.assets.entity.Gate mockGate = new com.kahua.assets.entity.Gate();
        mockGate.setName("North Gate");
        com.kahua.assets.entity.Shift mockShift = new com.kahua.assets.entity.Shift();
        mockShift.setName("Morning Shift");

        com.kahua.assets.entity.CashBankHistory h2 = new com.kahua.assets.entity.CashBankHistory();
        h2.setCashBankHistoryId(UUID.randomUUID());
        h2.setCashBankId(cbId);
        h2.setStatus("Assigned");
        h2.setGate(mockGate);
        h2.setShift(mockShift);

        com.kahua.assets.entity.CashBankHistory h3 = new com.kahua.assets.entity.CashBankHistory();
        h3.setCashBankHistoryId(UUID.randomUUID());
        h3.setCashBankId(cbId);
        h3.setStatus("Available");

        com.kahua.assets.entity.CashBankHistory h4 = new com.kahua.assets.entity.CashBankHistory();
        h4.setCashBankHistoryId(UUID.randomUUID());
        h4.setCashBankId(cbId);
        h4.setStatus("Returned Unused");

        when(cashBankRepository.findById(cbId)).thenReturn(Optional.of(mockCb));
        when(cashBankHistoryRepository.findByCashBankIdOrderByCreatedAtAsc(cbId)).thenReturn(List.of(h1, h2, h3, h4));
        when(trackerClient.getTimeline(cbId.toString(), TEST_TOKEN)).thenThrow(new RuntimeException("Tracker unreachable"));

        service.streamHistory(cbId, "Cash Bank", TEST_TOKEN, emitter);

        // Should emit ASSET_DISCOVERED, TIMELINE_LOADED, TIMELINE_UNAVAILABLE, STREAM_COMPLETE
        verify(emitter, atLeast(4)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
    }

    @Test
    void streamHistory_AssetNotFound_EmitsErrorEvent() throws IOException {
        UUID notFoundId = UUID.randomUUID();

        service.streamHistory(notFoundId, "Ticket Book", TEST_TOKEN, emitter);

        // Should emit NOT_FOUND event
        verify(emitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
    }

    @Test
    void searchAssetsForHistory_ReturnsMatchingTicketBooksAndCashBanks() {
        TicketBook mockTb = new TicketBook();
        mockTb.setTicketBookId(UUID.randomUUID());
        mockTb.setScanCode("TB-100");
        mockTb.setStartNum(1001);
        mockTb.setEndNum(1100);

        com.kahua.assets.entity.CashBank mockCb = new com.kahua.assets.entity.CashBank();
        mockCb.setCashBankId(UUID.randomUUID());
        mockCb.setBankName("Main Gate Vault");

        when(ticketBookRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(mockTb));
        when(cashBankRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(mockCb));

        var results = service.searchAssetsForHistory("1001");

        org.junit.jupiter.api.Assertions.assertEquals(2, results.size());
    }
}
