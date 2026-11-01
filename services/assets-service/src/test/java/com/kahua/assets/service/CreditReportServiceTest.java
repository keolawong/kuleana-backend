package com.kahua.assets.service;

import com.kahua.assets.dto.CreditReportDto;
import com.kahua.assets.dto.CreditReportPatchDto;
import com.kahua.assets.entity.CreditReport;
import com.kahua.assets.entity.Operation;
import com.kahua.assets.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditReportServiceTest {

    @Mock private CreditReportRepository creditReportRepository;
    @Mock private CreditReportHistoryRepository creditReportHistoryRepository;
    @Mock private OperationRepository operationRepository;
    @Mock private ShiftRepository shiftRepository;
    @Mock private GateRepository gateRepository;
    @Mock private PersonRepository personRepository;
    @Mock private PersonStickerRepository personStickerRepository;

    @InjectMocks
    private CreditReportService creditReportService;

    private UUID mockId;
    private CreditReport mockReport;

    @BeforeEach
    void setUp() {
        mockId = UUID.randomUUID();
        mockReport = new CreditReport();
        mockReport.setCreditReportId(mockId);
        mockReport.setTotalAmount(new BigDecimal("150.00"));
    }

    @Test
    void createCreditReport_shouldSaveAndLogHistory() {
        // Arrange
        CreditReportDto dto = new CreditReportDto();
        dto.setTotalAmount(new BigDecimal("200.00"));
        dto.setOperationId(UUID.randomUUID());

        Operation op = new Operation();
        when(operationRepository.findById(dto.getOperationId())).thenReturn(Optional.of(op));
        when(creditReportRepository.save(any(CreditReport.class))).thenReturn(mockReport);

        // Act
        CreditReport result = creditReportService.createCreditReport(dto);

        // Assert
        assertThat(result).isNotNull();
        verify(creditReportRepository).save(any(CreditReport.class));
        verify(creditReportHistoryRepository).save(any());
    }

    @Test
    void updateCreditReport_shouldUpdateDirtyFieldsAndLogHistory() {
        // Arrange
        CreditReportPatchDto patch = mock(CreditReportPatchDto.class);
        when(patch.isDirty("totalAmount")).thenReturn(true);
        when(patch.getTotalAmount()).thenReturn(new BigDecimal("500.00"));

        when(creditReportRepository.findById(mockId)).thenReturn(Optional.of(mockReport));
        when(creditReportRepository.save(any(CreditReport.class))).thenReturn(mockReport);

        // Act
        CreditReport result = creditReportService.updateCreditReport(mockId, patch);

        // Assert
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("500.00"));
        verify(creditReportRepository).save(mockReport);
        verify(creditReportHistoryRepository).save(any());
    }

    @Test
    void updateCreditReport_shouldThrowIfNotFound() {
        // Arrange
        when(creditReportRepository.findById(mockId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> creditReportService.updateCreditReport(mockId, mock(CreditReportPatchDto.class)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("CreditReport not found");
    }
}
