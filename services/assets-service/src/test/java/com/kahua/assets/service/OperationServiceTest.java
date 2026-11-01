package com.kahua.assets.service;

import com.kahua.assets.dto.OperationDto;
import com.kahua.assets.entity.Operation;
import com.kahua.assets.repository.OperationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationServiceTest {

    @Mock
    private OperationRepository operationRepository;

    @InjectMocks
    private OperationService operationService;

    private OperationDto mockDto;
    private Operation mockSavedOperation;

    @BeforeEach
    void setUp() {
        mockDto = new OperationDto();
        mockDto.setName("Test Operation");
        mockDto.setStartDate(LocalDate.now());
        mockDto.setEndDate(LocalDate.now().plusDays(5));
        mockDto.setLocationName("Main Gate");
        mockDto.setIsActive(true);
        mockDto.setTicketPrice(new BigDecimal("15.00"));
        mockDto.setCcFee(new BigDecimal("1.50"));
        mockDto.setCcFeeType("FLAT");

        mockSavedOperation = new Operation();
        mockSavedOperation.setName("Test Operation");
        mockSavedOperation.setIsActive(true);
        mockSavedOperation.setCreatedBy("System");
    }

    @Test
    void createOperation_shouldSaveAndReturnOperation() {
        // Arrange
        when(operationRepository.save(any(Operation.class))).thenReturn(mockSavedOperation);

        // Act
        Operation result = operationService.createOperation(mockDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Operation");
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getCreatedBy()).isEqualTo("System");
    }
}
