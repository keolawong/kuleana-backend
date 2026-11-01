package com.kahua.assets.service;

import com.kahua.assets.dto.DeviceDto;
import com.kahua.assets.dto.DevicePatchDto;
import com.kahua.assets.entity.Device;
import com.kahua.assets.repository.CashBankRepository;
import com.kahua.assets.repository.CreditReportRepository;
import com.kahua.assets.repository.DeviceRepository;
import com.kahua.assets.repository.TicketBookRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private CashBankRepository cashBankRepository;

    @Mock
    private TicketBookRepository ticketBookRepository;

    @Mock
    private CreditReportRepository creditReportRepository;

    @InjectMocks
    private DeviceService deviceService;

    private UUID mockId;
    private Device mockDevice;

    @BeforeEach
    void setUp() {
        mockId = UUID.randomUUID();
        mockDevice = new Device();
        mockDevice.setId(mockId);
        mockDevice.setDeviceId("POS-100");
        mockDevice.setDeviceNumber("100");
        mockDevice.setIsActive(true);
        mockDevice.setCreatedBy("Tester");
    }

    @Test
    void createDevice_success() {
        DeviceDto dto = new DeviceDto();
        dto.setDeviceId("POS-100");
        dto.setDeviceNumber("100");

        when(deviceRepository.existsByDeviceId("POS-100")).thenReturn(false);
        when(deviceRepository.existsByDeviceNumber("100")).thenReturn(false);
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Device created = deviceService.createDevice(dto);

        assertThat(created).isNotNull();
        assertThat(created.getDeviceId()).isEqualTo("POS-100");
        assertThat(created.getDeviceNumber()).isEqualTo("100");
        assertThat(created.getIsActive()).isTrue();
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void bulkCreateDevices_success() {
        DeviceDto dto1 = new DeviceDto();
        dto1.setDeviceId("POS-101");
        dto1.setDeviceNumber("101");

        DeviceDto dto2 = new DeviceDto();
        dto2.setDeviceId("POS-102");
        dto2.setDeviceNumber("102");

        when(deviceRepository.existsByDeviceId("POS-101")).thenReturn(false);
        when(deviceRepository.existsByDeviceNumber("101")).thenReturn(false);
        when(deviceRepository.existsByDeviceId("POS-102")).thenReturn(false);
        when(deviceRepository.existsByDeviceNumber("102")).thenReturn(false);
        when(deviceRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Device> result = deviceService.bulkCreateDevices(List.of(dto1, dto2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDeviceId()).isEqualTo("POS-101");
        assertThat(result.get(1).getDeviceId()).isEqualTo("POS-102");
        verify(deviceRepository).saveAll(anyList());
    }

    @Test
    void bulkCreateDevices_emptyList_returnsEmpty() {
        List<Device> result = deviceService.bulkCreateDevices(List.of());
        assertThat(result).isEmpty();
        verify(deviceRepository, never()).saveAll(any());
    }

    @Test
    void bulkCreateDevices_duplicateWithinBatch_throwsException() {
        DeviceDto dto1 = new DeviceDto();
        dto1.setDeviceId("POS-101");
        dto1.setDeviceNumber("101");

        DeviceDto dto2 = new DeviceDto();
        dto2.setDeviceId("POS-101");
        dto2.setDeviceNumber("102");

        assertThrows(IllegalArgumentException.class, () -> deviceService.bulkCreateDevices(List.of(dto1, dto2)));
        verify(deviceRepository, never()).saveAll(any());
    }

    @Test
    void bulkCreateDevices_existsInDb_throwsException() {
        DeviceDto dto1 = new DeviceDto();
        dto1.setDeviceId("POS-101");
        dto1.setDeviceNumber("101");

        when(deviceRepository.existsByDeviceId("POS-101")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> deviceService.bulkCreateDevices(List.of(dto1)));
        verify(deviceRepository, never()).saveAll(any());
    }

    @Test
    void createDevice_duplicateDeviceId_throwsException() {
        DeviceDto dto = new DeviceDto();
        dto.setDeviceId("POS-100");
        dto.setDeviceNumber("100");

        when(deviceRepository.existsByDeviceId("POS-100")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> deviceService.createDevice(dto));
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void createDevice_duplicateDeviceNumber_throwsException() {
        DeviceDto dto = new DeviceDto();
        dto.setDeviceId("POS-100");
        dto.setDeviceNumber("100");

        when(deviceRepository.existsByDeviceId("POS-100")).thenReturn(false);
        when(deviceRepository.existsByDeviceNumber("100")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> deviceService.createDevice(dto));
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void createDevice_emptyFields_throwsException() {
        DeviceDto dto = new DeviceDto();
        dto.setDeviceId("");
        dto.setDeviceNumber("100");

        assertThrows(IllegalArgumentException.class, () -> deviceService.createDevice(dto));

        dto.setDeviceId("POS-100");
        dto.setDeviceNumber(" ");
        assertThrows(IllegalArgumentException.class, () -> deviceService.createDevice(dto));
    }

    @Test
    void updateDevice_success_andCascadesDeviceIdChange() {
        DevicePatchDto patch = new DevicePatchDto();
        patch.setDeviceId("POS-200");
        patch.setDeviceNumber("200");
        patch.setIsActive(false);

        when(deviceRepository.findById(mockId)).thenReturn(Optional.of(mockDevice));
        when(deviceRepository.existsByDeviceIdAndIdNot("POS-200", mockId)).thenReturn(false);
        when(deviceRepository.existsByDeviceNumberAndIdNot("200", mockId)).thenReturn(false);
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Device updated = deviceService.updateDevice(mockId, patch);

        assertThat(updated.getDeviceId()).isEqualTo("POS-200");
        assertThat(updated.getDeviceNumber()).isEqualTo("200");
        assertThat(updated.getIsActive()).isFalse();

        // Verify cascading updates to referencing tables
        verify(cashBankRepository).updateDeviceId("POS-100", "POS-200");
        verify(ticketBookRepository).updateDeviceId("POS-100", "POS-200");
        verify(creditReportRepository).updateDeviceId("POS-100", "POS-200");
    }

    @Test
    void updateDevice_duplicateDeviceId_throwsException() {
        DevicePatchDto patch = new DevicePatchDto();
        patch.setDeviceId("POS-DUPE");

        when(deviceRepository.findById(mockId)).thenReturn(Optional.of(mockDevice));
        when(deviceRepository.existsByDeviceIdAndIdNot("POS-DUPE", mockId)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> deviceService.updateDevice(mockId, patch));
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void updateDevice_duplicateDeviceNumber_throwsException() {
        DevicePatchDto patch = new DevicePatchDto();
        patch.setDeviceNumber("DUPE-NUM");

        when(deviceRepository.findById(mockId)).thenReturn(Optional.of(mockDevice));
        when(deviceRepository.existsByDeviceNumberAndIdNot("DUPE-NUM", mockId)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> deviceService.updateDevice(mockId, patch));
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void updateDevice_notFound_throwsEntityNotFoundException() {
        DevicePatchDto patch = new DevicePatchDto();
        when(deviceRepository.findById(mockId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> deviceService.updateDevice(mockId, patch));
    }

    @Test
    void searchDevices_returnsSortedResults() {
        when(deviceRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(mockDevice));

        List<Device> results = deviceService.searchDevices("POS", true, "createdAt", "desc");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDeviceId()).isEqualTo("POS-100");
        verify(deviceRepository).findAll(any(Specification.class), eq(Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Test
    void getDeviceById_success() {
        when(deviceRepository.findById(mockId)).thenReturn(Optional.of(mockDevice));

        Device found = deviceService.getDeviceById(mockId);
        assertThat(found).isEqualTo(mockDevice);
    }

    @Test
    void getDeviceById_notFound_throwsException() {
        when(deviceRepository.findById(mockId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> deviceService.getDeviceById(mockId));
    }

    @Test
    void deleteDevice_success() {
        when(deviceRepository.findById(mockId)).thenReturn(Optional.of(mockDevice));

        deviceService.deleteDevice(mockId);

        verify(deviceRepository).delete(mockDevice);
    }

    @Test
    void deleteDevice_notFound_throwsException() {
        when(deviceRepository.findById(mockId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> deviceService.deleteDevice(mockId));
    }
}
