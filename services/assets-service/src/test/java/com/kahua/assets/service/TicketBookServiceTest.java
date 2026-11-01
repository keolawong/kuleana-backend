package com.kahua.assets.service;

import com.kahua.assets.dto.TicketBookDto;
import com.kahua.assets.dto.TicketBookPatchDto;
import com.kahua.assets.entity.Operation;
import com.kahua.assets.entity.Shift;
import com.kahua.assets.entity.TicketBook;
import com.kahua.assets.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketBookServiceTest {

    @Mock private TicketBookRepository ticketBookRepository;
    @Mock private TicketBookHistoryRepository ticketBookHistoryRepository;
    @Mock private OperationRepository operationRepository;
    @Mock private ShiftRepository shiftRepository;
    @Mock private GateRepository gateRepository;
    @Mock private PersonRepository personRepository;
    @Mock private PersonStickerRepository personStickerRepository;

    @InjectMocks
    private TicketBookService ticketBookService;

    private TicketBook mockBook;
    private UUID mockBookId;

    @BeforeEach
    void setUp() {
        mockBookId = UUID.randomUUID();
        mockBook = new TicketBook();
        mockBook.setTicketBookId(mockBookId);
        mockBook.setScanCode("SCAN123");
        mockBook.setStartNum(100);
        mockBook.setEndNum(200);
        mockBook.setStatus("ASSIGNED");
    }

    @Test
    void createTicketBook_shouldSaveBookAndHistory() {
        // Arrange
        TicketBookDto dto = new TicketBookDto();
        dto.setScanCode("NEW_SCAN");
        dto.setStartNum(1);
        dto.setEndNum(50);
        dto.setStatus("UNASSIGNED");

        when(ticketBookRepository.save(any(TicketBook.class))).thenAnswer(i -> {
            TicketBook tb = i.getArgument(0);
            tb.setTicketBookId(UUID.randomUUID());
            return tb;
        });

        // Act
        TicketBook result = ticketBookService.createTicketBook(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getScanCode()).isEqualTo("NEW_SCAN");
        verify(ticketBookRepository, times(1)).save(any(TicketBook.class));
        verify(ticketBookHistoryRepository, times(1)).save(any());
    }

    @Test
    void splitTicketBooks_shouldSplitWhenNextTicketIsInBounds() {
        // Arrange
        mockBook.setNextTicket(150); // Inside 100-200
        Operation op = new Operation();
        mockBook.setOperation(op);

        when(ticketBookRepository.findById(mockBookId)).thenReturn(Optional.of(mockBook));
        when(ticketBookRepository.save(any(TicketBook.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        List<TicketBook> result = ticketBookService.splitTicketBooks(List.of(mockBookId));

        // Assert
        assertThat(result).hasSize(1);
        
        // Old book should be modified
        assertThat(mockBook.getEndNum()).isEqualTo(149);
        assertThat(mockBook.getStatus()).isEqualTo("COMPLETED");

        // New book should be created
        TicketBook newBook = result.get(0);
        assertThat(newBook.getStartNum()).isEqualTo(150);
        assertThat(newBook.getEndNum()).isEqualTo(200);
        assertThat(newBook.getNextTicket()).isNull();
        assertThat(newBook.getStatus()).isEqualTo("AVAILABLE");
        assertThat(newBook.getParentTicketBookId()).isEqualTo(mockBookId);

        verify(ticketBookRepository, times(2)).save(any(TicketBook.class));
        verify(ticketBookHistoryRepository, times(2)).save(any());
    }

    @Test
    void splitTicketBooks_shouldSkipWhenNextTicketIsOutOfBounds() {
        // Arrange
        mockBook.setNextTicket(250); // Out of bounds > 200

        when(ticketBookRepository.findById(mockBookId)).thenReturn(Optional.of(mockBook));

        // Act
        List<TicketBook> result = ticketBookService.splitTicketBooks(List.of(mockBookId));

        // Assert
        assertThat(result).isEmpty();
        verify(ticketBookRepository, never()).save(any(TicketBook.class));
    }

    @Test
    void updateTicketBook_shouldSucceedWhenStatusIsAvailable() {
        TicketBook existing = new TicketBook();
        existing.setTicketBookId(mockBookId);
        existing.setStatus("AVAILABLE");
        existing.setScanCode("SCAN_OLD");
        existing.setStartNum(100);
        existing.setEndNum(200);

        TicketBookPatchDto patchDto = new TicketBookPatchDto();
        patchDto.setScanCode("SCAN_NEW");
        patchDto.setStartNum(101);
        patchDto.setEndNum(250);

        when(ticketBookRepository.findById(mockBookId)).thenReturn(Optional.of(existing));
        when(ticketBookRepository.findByParentTicketBookId(mockBookId)).thenReturn(List.of());
        when(ticketBookRepository.save(any(TicketBook.class))).thenAnswer(i -> i.getArgument(0));

        TicketBook result = ticketBookService.updateTicketBook(mockBookId, patchDto);

        assertThat(result.getScanCode()).isEqualTo("SCAN_NEW");
        assertThat(result.getStartNum()).isEqualTo(101);
        assertThat(result.getEndNum()).isEqualTo(250);
        verify(ticketBookRepository, times(1)).save(existing);
    }

    @Test
    void updateTicketBook_shouldSucceedWhenStatusIsReturnedUnused() {
        TicketBook existing = new TicketBook();
        existing.setTicketBookId(mockBookId);
        existing.setStatus("Returned Unused");
        existing.setScanCode("SCAN_UNUSED");
        existing.setStartNum(100);
        existing.setEndNum(200);

        TicketBookPatchDto patchDto = new TicketBookPatchDto();
        patchDto.setScanCode("SCAN_EDITED");

        when(ticketBookRepository.findById(mockBookId)).thenReturn(Optional.of(existing));
        when(ticketBookRepository.findByParentTicketBookId(mockBookId)).thenReturn(List.of());
        when(ticketBookRepository.save(any(TicketBook.class))).thenAnswer(i -> i.getArgument(0));

        TicketBook result = ticketBookService.updateTicketBook(mockBookId, patchDto);

        assertThat(result.getScanCode()).isEqualTo("SCAN_EDITED");
        verify(ticketBookRepository, times(1)).save(existing);
    }

    @Test
    void updateTicketBook_shouldSucceedWhenAssignedOrUsed() {
        TicketBook existing = new TicketBook();
        existing.setTicketBookId(mockBookId);
        existing.setStatus("ASSIGNED");
        existing.setStartNum(100);
        existing.setEndNum(200);
        existing.setScanCode("SCAN_ASSIGNED");

        TicketBookPatchDto patchDto = new TicketBookPatchDto();
        patchDto.setScanCode("NEW_SCAN");
        patchDto.setEndNum(220);

        when(ticketBookRepository.findById(mockBookId)).thenReturn(Optional.of(existing));
        when(ticketBookRepository.findByParentTicketBookId(mockBookId)).thenReturn(List.of());
        when(ticketBookRepository.save(any(TicketBook.class))).thenAnswer(i -> i.getArgument(0));

        TicketBook result = ticketBookService.updateTicketBook(mockBookId, patchDto);

        assertThat(result.getScanCode()).isEqualTo("NEW_SCAN");
        assertThat(result.getEndNum()).isEqualTo(220);
        verify(ticketBookRepository, times(1)).save(existing);
    }

    @Test
    void updateTicketBook_shouldCascadeChildStartNumToParentEndNum() {
        UUID parentId = UUID.randomUUID();
        TicketBook parent = new TicketBook();
        parent.setTicketBookId(parentId);
        parent.setStartNum(100);
        parent.setEndNum(250);
        parent.setStatus("COMPLETED");

        TicketBook child = new TicketBook();
        child.setTicketBookId(mockBookId);
        child.setParentTicketBookId(parentId);
        child.setStartNum(251);
        child.setEndNum(500);
        child.setStatus("AVAILABLE");

        TicketBookPatchDto patchDto = new TicketBookPatchDto();
        patchDto.setStartNum(220); // Child start moves to 220 -> Parent end becomes 219

        when(ticketBookRepository.findById(mockBookId)).thenReturn(Optional.of(child));
        when(ticketBookRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(ticketBookRepository.save(any(TicketBook.class))).thenAnswer(i -> i.getArgument(0));

        TicketBook result = ticketBookService.updateTicketBook(mockBookId, patchDto);

        assertThat(result.getStartNum()).isEqualTo(220);
        assertThat(parent.getEndNum()).isEqualTo(219);
        verify(ticketBookRepository, times(1)).save(child);
        verify(ticketBookRepository, times(1)).save(parent);
    }

    @Test
    void updateTicketBook_shouldCascadeParentEndNumToChildStartNum() {
        UUID childId = UUID.randomUUID();
        TicketBook parent = new TicketBook();
        parent.setTicketBookId(mockBookId);
        parent.setStartNum(100);
        parent.setEndNum(250);
        parent.setStatus("COMPLETED");

        TicketBook child = new TicketBook();
        child.setTicketBookId(childId);
        child.setParentTicketBookId(mockBookId);
        child.setStartNum(251);
        child.setEndNum(500);
        child.setStatus("AVAILABLE");

        TicketBookPatchDto patchDto = new TicketBookPatchDto();
        patchDto.setEndNum(280); // Parent end moves to 280 -> Child start becomes 281

        when(ticketBookRepository.findById(mockBookId)).thenReturn(Optional.of(parent));
        when(ticketBookRepository.findByParentTicketBookId(mockBookId)).thenReturn(List.of(child));
        when(ticketBookRepository.save(any(TicketBook.class))).thenAnswer(i -> i.getArgument(0));

        TicketBook result = ticketBookService.updateTicketBook(mockBookId, patchDto);

        assertThat(result.getEndNum()).isEqualTo(280);
        assertThat(child.getStartNum()).isEqualTo(281);
        verify(ticketBookRepository, times(1)).save(parent);
        verify(ticketBookRepository, times(1)).save(child);
    }

    @Test
    void updateTicketBook_shouldCascadeScanCodeAcrossLineage() {
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();

        TicketBook parent = new TicketBook();
        parent.setTicketBookId(parentId);
        parent.setScanCode("OLD_SCAN");

        TicketBook current = new TicketBook();
        current.setTicketBookId(mockBookId);
        current.setParentTicketBookId(parentId);
        current.setScanCode("OLD_SCAN");

        TicketBook child = new TicketBook();
        child.setTicketBookId(childId);
        child.setParentTicketBookId(mockBookId);
        child.setScanCode("OLD_SCAN");

        TicketBookPatchDto patchDto = new TicketBookPatchDto();
        patchDto.setScanCode("GLOBAL_NEW_SCAN");

        when(ticketBookRepository.findById(mockBookId)).thenReturn(Optional.of(current));
        when(ticketBookRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(ticketBookRepository.findByParentTicketBookId(parentId)).thenReturn(List.of(current));
        when(ticketBookRepository.findByParentTicketBookId(mockBookId)).thenReturn(List.of(child));
        when(ticketBookRepository.findByParentTicketBookId(childId)).thenReturn(List.of());
        when(ticketBookRepository.save(any(TicketBook.class))).thenAnswer(i -> i.getArgument(0));

        TicketBook result = ticketBookService.updateTicketBook(mockBookId, patchDto);

        assertThat(result.getScanCode()).isEqualTo("GLOBAL_NEW_SCAN");
        assertThat(parent.getScanCode()).isEqualTo("GLOBAL_NEW_SCAN");
        assertThat(child.getScanCode()).isEqualTo("GLOBAL_NEW_SCAN");
    }

    @Test
    void updateTicketBook_shouldThrowWhenChildStartExceedsChildEnd() {
        UUID childId = UUID.randomUUID();
        TicketBook parent = new TicketBook();
        parent.setTicketBookId(mockBookId);
        parent.setStartNum(100);
        parent.setEndNum(250);

        TicketBook child = new TicketBook();
        child.setTicketBookId(childId);
        child.setParentTicketBookId(mockBookId);
        child.setStartNum(251);
        child.setEndNum(300);

        TicketBookPatchDto patchDto = new TicketBookPatchDto();
        patchDto.setEndNum(350); // Parent end 350 -> child start 351 > child end 300

        when(ticketBookRepository.findById(mockBookId)).thenReturn(Optional.of(parent));
        when(ticketBookRepository.findByParentTicketBookId(mockBookId)).thenReturn(List.of(child));

        assertThatThrownBy(() -> ticketBookService.updateTicketBook(mockBookId, patchDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceed its end number");
    }

    @Test
    void updateTicketBook_shouldThrowWhenParentEndLessThanParentStart() {
        UUID parentId = UUID.randomUUID();
        TicketBook parent = new TicketBook();
        parent.setTicketBookId(parentId);
        parent.setStartNum(100);
        parent.setEndNum(250);

        TicketBook child = new TicketBook();
        child.setTicketBookId(mockBookId);
        child.setParentTicketBookId(parentId);
        child.setStartNum(251);
        child.setEndNum(500);

        TicketBookPatchDto patchDto = new TicketBookPatchDto();
        patchDto.setStartNum(90); // Child start 90 -> parent end 89 < parent start 100

        when(ticketBookRepository.findById(mockBookId)).thenReturn(Optional.of(child));
        when(ticketBookRepository.findById(parentId)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> ticketBookService.updateTicketBook(mockBookId, patchDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("less than its start number");
    }

    @Test
    void updateTicketBook_shouldThrowWhenRangeInvalid() {
        TicketBook existing = new TicketBook();
        existing.setTicketBookId(mockBookId);
        existing.setStatus("AVAILABLE");
        existing.setStartNum(100);
        existing.setEndNum(200);

        TicketBookPatchDto patchDto = new TicketBookPatchDto();
        patchDto.setStartNum(300); // startNum 300 > endNum 200

        when(ticketBookRepository.findById(mockBookId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> ticketBookService.updateTicketBook(mockBookId, patchDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("endNum must be greater than or equal to startNum.");

        verify(ticketBookRepository, never()).save(any());
    }

    @Test
    void createTicketBooks_shouldBatchResolveEntitiesAndSaveAll() {
        UUID opId = UUID.randomUUID();
        Operation op = new Operation();
        op.setOperationId(opId);

        UUID shiftId = UUID.randomUUID();
        Shift shift = new Shift();
        shift.setShiftId(shiftId);

        TicketBookDto dto1 = new TicketBookDto();
        dto1.setScanCode("SCAN_BULK_1");
        dto1.setStartNum(1);
        dto1.setEndNum(50);
        dto1.setOperationId(opId);
        dto1.setShiftId(shiftId);

        TicketBookDto dto2 = new TicketBookDto();
        dto2.setScanCode("SCAN_BULK_2");
        dto2.setStartNum(51);
        dto2.setEndNum(100);
        dto2.setOperationId(opId);
        dto2.setShiftId(shiftId);

        when(operationRepository.findAllById(anySet())).thenReturn(List.of(op));
        when(shiftRepository.findAllById(anySet())).thenReturn(List.of(shift));
        when(ticketBookRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<TicketBook> created = ticketBookService.createTicketBooks(List.of(dto1, dto2));

        assertThat(created).hasSize(2);
        assertThat(created.get(0).getOperation()).isEqualTo(op);
        assertThat(created.get(1).getShift()).isEqualTo(shift);
        verify(operationRepository, times(1)).findAllById(anySet());
        verify(shiftRepository, times(1)).findAllById(anySet());
        verify(operationRepository, never()).findById(any());
        verify(shiftRepository, never()).findById(any());
        verify(ticketBookRepository, times(1)).saveAll(anyList());
        verify(ticketBookHistoryRepository, times(2)).save(any());
    }

    @Test
    void createTicketBooks_shouldThrowWhenOperationNotFound() {
        UUID opId = UUID.randomUUID();
        TicketBookDto dto = new TicketBookDto();
        dto.setOperationId(opId);

        when(operationRepository.findAllById(anySet())).thenReturn(List.of());

        assertThatThrownBy(() -> ticketBookService.createTicketBooks(List.of(dto)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Operation not found with id: " + opId);
    }
}
