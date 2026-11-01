package com.kahua.assets.service;

import com.kahua.assets.dto.CashBankDto;
import com.kahua.assets.dto.CashBankPatchDto;
import com.kahua.assets.entity.CashBank;
import com.kahua.assets.entity.Operation;
import com.kahua.assets.entity.Shift;
import com.kahua.assets.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashBankServiceTest {

    @Mock private CashBankRepository cashBankRepository;
    @Mock private CashBankHistoryRepository cashBankHistoryRepository;
    @Mock private OperationRepository operationRepository;
    @Mock private ShiftRepository shiftRepository;
    @Mock private GateRepository gateRepository;
    @Mock private PersonRepository personRepository;
    @Mock private PersonStickerRepository personStickerRepository;

    @InjectMocks
    private CashBankService cashBankService;

    private CashBankDto mockDto;
    private UUID mockOperationId;

    @BeforeEach
    void setUp() {
        mockOperationId = UUID.randomUUID();
        mockDto = new CashBankDto();
        mockDto.setBankName("Vault Bank 1");
        mockDto.setStartingAmount(new BigDecimal("1000.00"));
        mockDto.setOperationId(mockOperationId);
        mockDto.setStatus("UNASSIGNED");
    }

    @Test
    void createCashBank_shouldSaveCashBankAndHistory() {
        // Arrange
        Operation mockOp = new Operation();
        mockOp.setOperationId(mockOperationId);
        mockDto.setCount1cs(25);

        CashBank savedBank = new CashBank();
        savedBank.setCashBankId(UUID.randomUUID());
        savedBank.setBankName("Vault Bank 1");
        savedBank.setCount1cs(25);
        savedBank.setCreatedBy("System");

        when(operationRepository.findById(mockOperationId)).thenReturn(Optional.of(mockOp));
        when(cashBankRepository.save(any(CashBank.class))).thenReturn(savedBank);

        // Act
        CashBank result = cashBankService.createCashBank(mockDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getBankName()).isEqualTo("Vault Bank 1");
        assertThat(result.getCount1cs()).isEqualTo(25);
        verify(cashBankRepository, times(1)).save(any(CashBank.class));
        verify(cashBankHistoryRepository, times(1)).save(any());
    }

    @Test
    void createCashBank_shouldThrowExceptionIfOperationNotFound() {
        // Arrange
        when(operationRepository.findById(mockOperationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cashBankService.createCashBank(mockDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Operation not found");
        
        verify(cashBankRepository, never()).save(any());
    }

    @Test
    void updateCashBank_shouldUpdateDirtyFields() {
        // Arrange
        UUID cashBankId = UUID.randomUUID();
        CashBank existingBank = new CashBank();
        existingBank.setBankName("Old Name");
        existingBank.setStartingAmount(new BigDecimal("500.00"));

        CashBankPatchDto patchMock = mock(CashBankPatchDto.class);
        when(patchMock.isDirty("bankName")).thenReturn(true);
        when(patchMock.getBankName()).thenReturn("New Name");
        // All other isDirty calls return false by default

        when(cashBankRepository.findById(cashBankId)).thenReturn(Optional.of(existingBank));
        when(cashBankRepository.save(any(CashBank.class))).thenReturn(existingBank);

        // Act
        CashBank result = cashBankService.updateCashBank(cashBankId, patchMock);

        // Assert
        assertThat(result.getBankName()).isEqualTo("New Name");
        // startingAmount shouldn't change because it wasn't dirty
        assertThat(result.getStartingAmount()).isEqualTo(new BigDecimal("500.00"));
        verify(cashBankHistoryRepository, times(1)).save(any());
    }

    @Test
    void updateCashBank_shouldUpdateCount1cs() {
        // Arrange
        UUID cashBankId = UUID.randomUUID();
        CashBank existingBank = new CashBank();
        existingBank.setCount1cs(0);

        CashBankPatchDto patchDto = new CashBankPatchDto();
        patchDto.setCount1cs(100);

        when(cashBankRepository.findById(cashBankId)).thenReturn(Optional.of(existingBank));
        when(cashBankRepository.save(any(CashBank.class))).thenReturn(existingBank);

        // Act
        CashBank result = cashBankService.updateCashBank(cashBankId, patchDto);

        // Assert
        assertThat(result.getCount1cs()).isEqualTo(100);
        verify(cashBankHistoryRepository, times(1)).save(argThat(history -> Integer.valueOf(100).equals(history.getCount1cs())));
    }

    @Test
    void updateCashBank_shouldSucceedWhenStatusIsAvailable() {
        UUID cashBankId = UUID.randomUUID();
        CashBank existingBank = new CashBank();
        existingBank.setStatus("AVAILABLE");
        existingBank.setBankName("Old Available Bank");
        existingBank.setStartingAmount(new BigDecimal("200.00"));

        CashBankPatchDto patchDto = new CashBankPatchDto();
        patchDto.setBankName("Updated Available Bank");
        patchDto.setStartingAmount(new BigDecimal("300.00"));

        when(cashBankRepository.findById(cashBankId)).thenReturn(Optional.of(existingBank));
        when(cashBankRepository.save(any(CashBank.class))).thenAnswer(i -> i.getArgument(0));

        CashBank result = cashBankService.updateCashBank(cashBankId, patchDto);

        assertThat(result.getBankName()).isEqualTo("Updated Available Bank");
        assertThat(result.getStartingAmount()).isEqualTo(new BigDecimal("300.00"));
        verify(cashBankRepository, times(1)).save(existingBank);
    }

    @Test
    void updateCashBank_shouldSucceedWhenStatusIsReturnedUnused() {
        UUID cashBankId = UUID.randomUUID();
        CashBank existingBank = new CashBank();
        existingBank.setStatus("Returned Unused");
        existingBank.setBankName("Old Unused Bank");
        existingBank.setStartingAmount(new BigDecimal("200.00"));

        CashBankPatchDto patchDto = new CashBankPatchDto();
        patchDto.setBankName("New Unused Bank Name");

        when(cashBankRepository.findById(cashBankId)).thenReturn(Optional.of(existingBank));
        when(cashBankRepository.save(any(CashBank.class))).thenAnswer(i -> i.getArgument(0));

        CashBank result = cashBankService.updateCashBank(cashBankId, patchDto);

        assertThat(result.getBankName()).isEqualTo("New Unused Bank Name");
        verify(cashBankRepository, times(1)).save(existingBank);
    }

    @Test
    void updateCashBank_shouldThrowWhenAssigned() {
        UUID cashBankId = UUID.randomUUID();
        CashBank existingBank = new CashBank();
        existingBank.setStatus("ASSIGNED");
        existingBank.setBankName("Assigned Bank");

        CashBankPatchDto patchDto = new CashBankPatchDto();
        patchDto.setBankName("Attempted New Name");

        when(cashBankRepository.findById(cashBankId)).thenReturn(Optional.of(existingBank));

        assertThatThrownBy(() -> cashBankService.updateCashBank(cashBankId, patchDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot edit a cash bank that is currently assigned.");

        verify(cashBankRepository, never()).save(any());
    }

    @Test
    void updateCashBank_shouldThrowWhenUsedOrReconciled_withReturnedStatus() {
        UUID cashBankId = UUID.randomUUID();
        CashBank existingBank = new CashBank();
        existingBank.setStatus("RETURNED");
        existingBank.setBankName("Returned Bank");

        CashBankPatchDto patchDto = new CashBankPatchDto();
        patchDto.setStartingAmount(new BigDecimal("500.00"));

        when(cashBankRepository.findById(cashBankId)).thenReturn(Optional.of(existingBank));

        assertThatThrownBy(() -> cashBankService.updateCashBank(cashBankId, patchDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot edit a cash bank that has already been used or reconciled.");

        verify(cashBankRepository, never()).save(any());
    }

    @Test
    void updateCashBank_shouldThrowWhenUsedOrReconciled_withCounts() {
        UUID cashBankId = UUID.randomUUID();
        CashBank existingBank = new CashBank();
        existingBank.setStatus("AVAILABLE");
        existingBank.setCount20s(5);

        CashBankPatchDto patchDto = new CashBankPatchDto();
        patchDto.setBankName("Modified Bank");

        when(cashBankRepository.findById(cashBankId)).thenReturn(Optional.of(existingBank));

        assertThatThrownBy(() -> cashBankService.updateCashBank(cashBankId, patchDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot edit a cash bank that has already been used or reconciled.");

        verify(cashBankRepository, never()).save(any());
    }

    @Test
    void updateCashBank_shouldThrowWhenUsedOrReconciled_withOfficialCount() {
        UUID cashBankId = UUID.randomUUID();
        CashBank existingBank = new CashBank();
        existingBank.setStatus("AVAILABLE");
        existingBank.setOfficialCount(new BigDecimal("100.00"));

        CashBankPatchDto patchDto = new CashBankPatchDto();
        patchDto.setStartingAmount(new BigDecimal("150.00"));

        when(cashBankRepository.findById(cashBankId)).thenReturn(Optional.of(existingBank));

        assertThatThrownBy(() -> cashBankService.updateCashBank(cashBankId, patchDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot edit a cash bank that has already been used or reconciled.");

        verify(cashBankRepository, never()).save(any());
    }

    @Test
    void createCashBanks_shouldBatchResolveEntitiesAndSaveAll() {
        UUID opId = UUID.randomUUID();
        Operation op = new Operation();
        op.setOperationId(opId);

        UUID shiftId = UUID.randomUUID();
        Shift shift = new Shift();
        shift.setShiftId(shiftId);

        CashBankDto dto1 = new CashBankDto();
        dto1.setBankName("Bank 1");
        dto1.setStartingAmount(new BigDecimal("100.00"));
        dto1.setOperationId(opId);
        dto1.setShiftId(shiftId);

        CashBankDto dto2 = new CashBankDto();
        dto2.setBankName("Bank 2");
        dto2.setStartingAmount(new BigDecimal("200.00"));
        dto2.setOperationId(opId);
        dto2.setShiftId(shiftId);

        when(operationRepository.findAllById(anySet())).thenReturn(List.of(op));
        when(shiftRepository.findAllById(anySet())).thenReturn(List.of(shift));
        when(cashBankRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<CashBank> created = cashBankService.createCashBanks(List.of(dto1, dto2));

        assertThat(created).hasSize(2);
        assertThat(created.get(0).getOperation()).isEqualTo(op);
        assertThat(created.get(1).getShift()).isEqualTo(shift);
        verify(operationRepository, times(1)).findAllById(anySet());
        verify(shiftRepository, times(1)).findAllById(anySet());
        verify(operationRepository, never()).findById(any());
        verify(shiftRepository, never()).findById(any());
        verify(cashBankRepository, times(1)).saveAll(anyList());
        verify(cashBankHistoryRepository, times(2)).save(any());
    }

    @Test
    void createCashBanks_shouldThrowWhenOperationNotFound() {
        UUID opId = UUID.randomUUID();
        CashBankDto dto = new CashBankDto();
        dto.setOperationId(opId);

        when(operationRepository.findAllById(anySet())).thenReturn(List.of());

        assertThatThrownBy(() -> cashBankService.createCashBanks(List.of(dto)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Operation not found with id: " + opId);
    }

    @Test
    void updateCashBank_shouldResolvePersonByPersonStickerIdWhenNotFoundInPersonRepository() {
        UUID cashBankId = UUID.randomUUID();
        UUID stickerId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();

        com.kahua.assets.entity.Person person = new com.kahua.assets.entity.Person();
        person.setPersonId(personId);

        com.kahua.assets.entity.PersonSticker sticker = new com.kahua.assets.entity.PersonSticker();
        sticker.setPersonStickerId(stickerId);
        sticker.setPerson(person);

        CashBank existingBank = new CashBank();
        existingBank.setCashBankId(cashBankId);
        existingBank.setStatus("ASSIGNED");

        CashBankPatchDto patchDto = new CashBankPatchDto();
        patchDto.setPersonStickerId(stickerId);

        when(cashBankRepository.findById(cashBankId)).thenReturn(Optional.of(existingBank));
        when(personRepository.findById(stickerId)).thenReturn(Optional.empty());
        when(personStickerRepository.findById(stickerId)).thenReturn(Optional.of(sticker));
        when(cashBankRepository.save(any(CashBank.class))).thenAnswer(i -> i.getArgument(0));

        CashBank updated = cashBankService.updateCashBank(cashBankId, patchDto);

        assertThat(updated.getPerson()).isEqualTo(person);
        verify(personRepository, times(1)).findById(stickerId);
        verify(personStickerRepository, times(1)).findById(stickerId);
    }
}
