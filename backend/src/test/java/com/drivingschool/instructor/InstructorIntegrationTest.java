package com.drivingschool.instructor;

import com.drivingschool.auth.EmailAlreadyRegisteredException;
import com.drivingschool.instructor.dto.CreateInstructorRequest;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class InstructorIntegrationTest {

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
    private InstructorService instructorService;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migratesSearchesPaginatesAndEnforcesUniqueEmailAndLicense() {
        String marker = UUID.randomUUID().toString().substring(0, 8);
        var first = instructorService.create(request(
                "Sophie", "Search", "sophie-" + marker + "@example.com", "LIC-" + marker + "-A"
        ));
        var second = instructorService.create(request(
                "Alex", "Search", "alex-" + marker + "@example.com", "LIC-" + marker + "-B"
        ));
        var pageable = PageRequest.of(0, 1, Sort.by("lastName").ascending());

        var allSearchMatches = instructorService.findAll(marker, null, pageable);
        var licenseMatch = instructorService.findAll("lic-" + marker + "-a", null, pageable);
        var inactiveMatches = instructorService.findAll(marker, InstructorStatus.INACTIVE, pageable);

        assertThat(jdbcTemplate.queryForObject("SELECT to_regclass('public.instructors')", String.class))
                .isEqualTo("instructors");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'instructors' "
                        + "AND indexname IN ('idx_instructors_last_first_name', 'idx_instructors_status')",
                Integer.class
        )).isEqualTo(2);
        assertThat(allSearchMatches.getTotalElements()).isEqualTo(2);
        assertThat(allSearchMatches.getContent()).hasSize(1);
        assertThat(licenseMatch.getTotalElements()).isEqualTo(1);
        assertThat(licenseMatch.getContent().getFirst().id()).isEqualTo(first.id());
        assertThat(inactiveMatches.getTotalElements()).isZero();

        assertThatThrownBy(() -> instructorService.create(request(
                "Duplicate", "Email", first.email(), "LIC-" + marker + "-C"
        ))).isInstanceOf(EmailAlreadyRegisteredException.class);
        assertThatThrownBy(() -> instructorService.create(request(
                "Duplicate", "License", "duplicate-" + marker + "@example.com", first.licenseNumber()
        ))).isInstanceOf(InstructorLicenseAlreadyRegisteredException.class);

        assertThat(instructorRepository.findById(second.id())).isPresent();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE id = ?",
                String.class,
                first.userId()
        )).startsWith("$2");
    }

    private CreateInstructorRequest request(String firstName, String lastName, String email, String license) {
        return new CreateInstructorRequest(
                firstName,
                lastName,
                "+21612345678",
                email,
                license,
                "integration-password"
        );
    }
}
