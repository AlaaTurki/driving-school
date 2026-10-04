package com.drivingschool.vehicle;

import com.drivingschool.vehicle.dto.SaveVehicleRequest;
import com.drivingschool.vehicle.dto.VehicleResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VehicleServiceTest {

    private final VehicleRepository vehicleRepository = mock(VehicleRepository.class);
    private final VehicleService service = new VehicleService(vehicleRepository);

    @Test
    void createsVehicleWithNormalizedRegistrationNumber() {
        when(vehicleRepository.existsByRegistrationNumberIgnoreCase("ABC-123")).thenReturn(false);
        when(vehicleRepository.saveAndFlush(any(Vehicle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = service.create(request("  abc-123  "));

        var captor = org.mockito.ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getRegistrationNumber()).isEqualTo("ABC-123");
        assertThat(response.registrationNumber()).isEqualTo("ABC-123");
    }

    @Test
    void rejectsCaseInsensitiveDuplicateRegistrationNumber() {
        when(vehicleRepository.existsByRegistrationNumberIgnoreCase("ABC-123")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("abc-123")))
                .isInstanceOf(VehicleRegistrationAlreadyExistsException.class);

        verify(vehicleRepository, never()).saveAndFlush(any(Vehicle.class));
    }

    @Test
    void trimsSearchAndAppliesActiveFilter() {
        var pageable = PageRequest.of(0, 10);
        when(vehicleRepository.search("toyota", false, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.findAll(" toyota ", false, pageable);

        verify(vehicleRepository).search("toyota", false, pageable);
    }

    private SaveVehicleRequest request(String registrationNumber) {
        return new SaveVehicleRequest(registrationNumber, "Toyota", "Yaris", VehicleType.MANUAL, true);
    }
}
