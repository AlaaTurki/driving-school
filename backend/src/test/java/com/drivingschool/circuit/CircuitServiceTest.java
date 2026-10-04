package com.drivingschool.circuit;

import com.drivingschool.circuit.dto.SaveCircuitRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CircuitServiceTest {

    private final CircuitRepository circuitRepository = mock(CircuitRepository.class);
    private final CircuitService service = new CircuitService(circuitRepository);

    @Test
    void trimsCircuitFieldsAndAcceptsZeroPrice() {
        when(circuitRepository.existsByNameIgnoreCase("City Loop")).thenReturn(false);
        when(circuitRepository.saveAndFlush(any(Circuit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(request(" City Loop ", "  ", " Downtown ", "0.000"));

        var captor = org.mockito.ArgumentCaptor.forClass(Circuit.class);
        verify(circuitRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("City Loop");
        assertThat(captor.getValue().getDescription()).isNull();
        assertThat(captor.getValue().getLocation()).isEqualTo("Downtown");
        assertThat(response.price()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void rejectsDuplicateCircuitName() {
        when(circuitRepository.existsByNameIgnoreCase("City Loop")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("City Loop", null, null, "12.500")))
                .isInstanceOf(CircuitNameAlreadyExistsException.class);

        verify(circuitRepository, never()).saveAndFlush(any(Circuit.class));
    }

    @Test
    void rejectsNegativePriceAtServiceBoundary() {
        assertThatThrownBy(() -> service.create(request("City Loop", null, null, "-0.001")))
                .isInstanceOf(InvalidCircuitPriceException.class);

        verify(circuitRepository, never()).saveAndFlush(any(Circuit.class));
    }

    @Test
    void rejectsPriceWithMoreThanThreeDecimalPlaces() {
        assertThatThrownBy(() -> service.create(request("City Loop", null, null, "1.0001")))
                .isInstanceOf(InvalidCircuitPriceException.class);
    }

    @Test
    void trimsSearchAndAppliesActiveFilter() {
        var pageable = PageRequest.of(0, 10);
        when(circuitRepository.search("downtown", false, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.findAll(" downtown ", false, pageable);

        verify(circuitRepository).search("downtown", false, pageable);
    }

    private SaveCircuitRequest request(String name, String description, String location, String price) {
        return new SaveCircuitRequest(name, description, location, new BigDecimal(price), true);
    }
}
