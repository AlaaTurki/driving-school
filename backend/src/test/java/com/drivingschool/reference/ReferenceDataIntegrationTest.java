package com.drivingschool.reference;

import com.drivingschool.circuit.CircuitNameAlreadyExistsException;
import com.drivingschool.circuit.CircuitService;
import com.drivingschool.circuit.dto.SaveCircuitRequest;
import com.drivingschool.vehicle.VehicleRegistrationAlreadyExistsException;
import com.drivingschool.vehicle.VehicleService;
import com.drivingschool.vehicle.VehicleType;
import com.drivingschool.vehicle.dto.SaveVehicleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ReferenceDataIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("app.jwt.secret", () -> java.util.Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private CircuitService circuitService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migratesSearchesPaginatesAndFiltersActiveReferenceData() {
        String marker = UUID.randomUUID().toString().substring(0, 8);
        var activeVehicle = vehicleService.create(new SaveVehicleRequest(
                "  " + marker + "-a ", "Toyota", "Yaris", VehicleType.MANUAL, true
        ));
        var inactiveVehicle = vehicleService.create(new SaveVehicleRequest(
                marker + "-b", "Honda", "Civic", VehicleType.AUTOMATIC, false
        ));
        var activeCircuit = circuitService.create(new SaveCircuitRequest(
                "Circuit " + marker + " A", "Practice route", "North district", new BigDecimal("12.500"), true
        ));
        circuitService.create(new SaveCircuitRequest(
                "Circuit " + marker + " B", null, "South district", BigDecimal.ZERO, false
        ));

        var vehiclePage = vehicleService.findAll(marker, null, PageRequest.of(
                0, 1, Sort.by("registrationNumber").ascending()
        ));
        var vehicleSecondPage = vehicleService.findAll(marker, null, PageRequest.of(
                1, 1, Sort.by("registrationNumber").ascending()
        ));
        var activeVehicles = vehicleService.findAll(marker, true, PageRequest.of(0, 20));
        var inactiveVehicles = vehicleService.findAll(marker, false, PageRequest.of(0, 20));
        var circuitPage = circuitService.findAll(marker, null, PageRequest.of(
                0, 1, Sort.by("name").ascending()
        ));
        var activeCircuits = circuitService.findAll(marker, true, PageRequest.of(0, 20));
        var locationMatch = circuitService.findAll("north district", null, PageRequest.of(0, 20));

        assertThat(jdbcTemplate.queryForObject("SELECT to_regclass('public.vehicles')", String.class))
                .isEqualTo("vehicles");
        assertThat(jdbcTemplate.queryForObject("SELECT to_regclass('public.circuits')", String.class))
                .isEqualTo("circuits");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE indexname IN ('idx_vehicles_active', 'idx_circuits_active')",
                Integer.class
        )).isEqualTo(2);
        assertThat(activeVehicle.registrationNumber()).isEqualTo(marker.toUpperCase() + "-A");
        assertThat(vehiclePage.getTotalElements()).isEqualTo(2);
        assertThat(vehiclePage.getTotalPages()).isEqualTo(2);
        assertThat(vehicleSecondPage.getContent()).extracting("id").containsExactly(inactiveVehicle.id());
        assertThat(activeVehicles.getContent()).extracting("id").containsExactly(activeVehicle.id());
        assertThat(inactiveVehicles.getContent()).extracting("id").containsExactly(inactiveVehicle.id());
        assertThat(circuitPage.getTotalElements()).isEqualTo(2);
        assertThat(circuitPage.getTotalPages()).isEqualTo(2);
        assertThat(activeCircuits.getContent()).extracting("id").containsExactly(activeCircuit.id());
        assertThat(locationMatch.getTotalElements()).isEqualTo(1);

        assertThatThrownBy(() -> vehicleService.create(new SaveVehicleRequest(
                marker + "-A", "Other", "Model", VehicleType.MANUAL, true
        ))).isInstanceOf(VehicleRegistrationAlreadyExistsException.class);
        assertThatThrownBy(() -> circuitService.create(new SaveCircuitRequest(
                " circuit " + marker + " a ", null, null, BigDecimal.ONE, true
        ))).isInstanceOf(CircuitNameAlreadyExistsException.class);
    }
}
