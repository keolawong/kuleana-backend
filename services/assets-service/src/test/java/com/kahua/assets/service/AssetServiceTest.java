package com.kahua.assets.service;

import com.kahua.assets.dto.AssetSearchResponseDto;
import com.kahua.assets.entity.CashBank;
import com.kahua.assets.entity.Operation;
import com.kahua.assets.entity.TicketBook;
import com.kahua.assets.repository.CashBankRepository;
import com.kahua.assets.repository.TicketBookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private CashBankRepository cashBankRepository;

    @Mock
    private TicketBookRepository ticketBookRepository;

    @Mock
    private com.kahua.assets.repository.DeviceRepository deviceRepository;

    @InjectMocks
    private AssetService assetService;

    private UUID mockOperationId;
    private CashBank mockCashBank;
    private TicketBook mockTicketBook;
    private com.kahua.assets.entity.Device mockDevice;

    @BeforeEach
    void setUp() {
        mockOperationId = UUID.randomUUID();

        Operation op = new Operation();
        op.setOperationId(mockOperationId);

        mockCashBank = new CashBank();
        mockCashBank.setCashBankId(UUID.randomUUID());
        mockCashBank.setBankName("Test Bank");
        mockCashBank.setStatus("UNASSIGNED");
        mockCashBank.setOperation(op);

        mockTicketBook = new TicketBook();
        mockTicketBook.setTicketBookId(UUID.randomUUID());
        mockTicketBook.setScanCode("TB123");
        mockTicketBook.setStatus("UNASSIGNED");
        mockTicketBook.setOperation(op);

        mockDevice = new com.kahua.assets.entity.Device();
        mockDevice.setId(UUID.randomUUID());
        mockDevice.setDeviceId("DEV-1");
        mockDevice.setDeviceNumber("01");
        mockDevice.setIsActive(true);
    }

    @Test
    void searchAssets_shouldReturnMappedDto() {
        // Arrange
        when(cashBankRepository.findAll(any(Specification.class))).thenReturn(List.of(mockCashBank));
        when(ticketBookRepository.findAll(any(Specification.class))).thenReturn(List.of(mockTicketBook));
        when(deviceRepository.findAll(any(Specification.class))).thenReturn(List.of(mockDevice));

        // Act
        AssetSearchResponseDto response = assetService.searchAssets(mockOperationId, "Test");

        // Assert
        assertThat(response).isNotNull();
        
        assertThat(response.getCashBank()).hasSize(1);
        assertThat(response.getCashBank().get(0).getBankName()).isEqualTo("Test Bank");
        assertThat(response.getCashBank().get(0).getOperationId()).isEqualTo(mockOperationId);

        assertThat(response.getTicketBook()).hasSize(1);
        assertThat(response.getTicketBook().get(0).getScanCode()).isEqualTo("TB123");
        assertThat(response.getTicketBook().get(0).getOperationId()).isEqualTo(mockOperationId);

        assertThat(response.getDevice()).hasSize(1);
        assertThat(response.getDevice().get(0).getDeviceId()).isEqualTo("DEV-1");
        assertThat(response.getDevice().get(0).getDeviceNumber()).isEqualTo("01");
    }
}
