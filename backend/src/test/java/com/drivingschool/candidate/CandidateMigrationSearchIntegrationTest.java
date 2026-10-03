package com.drivingschool.candidate;

import com.drivingschool.candidate.dto.CandidateResponse;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
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

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CandidateMigrationSearchIntegrationTest {

    private static final UUID LEGACY_USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER_USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    private static boolean legacySchemaPrepared;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry properties) {
        prepareLegacySchemaAndMigrate();
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("app.jwt.secret", () -> java.util.Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationMovesLegacyRowsAndDropsCandidateProfiles() {
        Candidate migrated = candidateRepository.findById(LEGACY_USER_ID).orElseThrow();

        assertThat(migrated.getUser().getId()).isEqualTo(LEGACY_USER_ID);
        assertThat(migrated.getFirstName()).isEqualTo("Sophie");
        assertThat(migrated.getLastName()).isEqualTo("Legacy Candidate");
        assertThat(migrated.getEmail()).isEqualTo("sophie.legacy@example.com");
        assertThat(migrated.getPhone()).isEqualTo("+21612345678");
        assertThat(migrated.getRegistrationDate()).isEqualTo(LocalDate.of(2024, 5, 6));
        assertThat(migrated.getStatus()).isEqualTo(CandidateStatus.INACTIVE);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.candidate_profiles')",
                String.class
        )).isNull();
    }

    @Test
    void searchesByNameEmailAndPhoneAndFiltersByStatusWithPagination() {
        var pageable = PageRequest.of(0, 1, Sort.by("lastName").ascending());

        var allCandidates = candidateService.findAll(null, null, pageable);
        var nameMatch = candidateService.findAll("legacy candidate", null, pageable);
        var emailMatch = candidateService.findAll("SOPHIE.LEGACY@", null, pageable);
        var phoneMatch = candidateService.findAll("12345678", null, pageable);
        var activeMatch = candidateService.findAll("Sophie", CandidateStatus.ACTIVE, pageable);
        var inactiveMatch = candidateService.findAll("Sophie", CandidateStatus.INACTIVE, pageable);

        assertThat(allCandidates.getTotalElements()).isEqualTo(2);
        assertThat(nameMatch.getTotalElements()).isEqualTo(1);
        assertThat(emailMatch.getTotalElements()).isEqualTo(1);
        assertThat(phoneMatch.getTotalElements()).isEqualTo(1);
        assertThat(activeMatch.getTotalElements()).isZero();
        assertThat(inactiveMatch.getContent()).extracting(CandidateResponse::email)
                .containsExactly("sophie.legacy@example.com");
    }

    private static synchronized void prepareLegacySchemaAndMigrate() {
        if (legacySchemaPrepared) {
            return;
        }
        String url = POSTGRES.getJdbcUrl();
        String username = POSTGRES.getUsername();
        String password = POSTGRES.getPassword();

        Flyway.configure()
                .dataSource(url, username, password)
                .target(MigrationVersion.fromVersion("3"))
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(url, username, password);
             var userStatement = connection.prepareStatement("""
                     INSERT INTO users (id, email, password_hash, enabled, created_at, updated_at)
                     VALUES (?, ?, ?, ?, ?, ?)
                     """);
             var roleStatement = connection.prepareStatement(
                     "INSERT INTO user_roles (user_id, role) VALUES (?, 'CANDIDATE')");
             var profileStatement = connection.prepareStatement(
                     "INSERT INTO candidate_profiles (user_id, full_name, phone) VALUES (?, ?, ?)")) {
            userStatement.setObject(1, LEGACY_USER_ID);
            userStatement.setString(2, "sophie.legacy@example.com");
            userStatement.setString(3, "not-a-real-password-hash");
            userStatement.setBoolean(4, false);
            userStatement.setObject(5, java.time.OffsetDateTime.parse("2024-05-06T12:00:00Z"));
            userStatement.setObject(6, java.time.OffsetDateTime.parse("2024-05-07T12:00:00Z"));
            userStatement.executeUpdate();
            roleStatement.setObject(1, LEGACY_USER_ID);
            roleStatement.executeUpdate();
            profileStatement.setObject(1, LEGACY_USER_ID);
            profileStatement.setString(2, "Sophie Legacy Candidate");
            profileStatement.setString(3, "+21612345678");
            profileStatement.executeUpdate();

            userStatement.setObject(1, OTHER_USER_ID);
            userStatement.setString(2, "active.person@example.com");
            userStatement.setString(3, "not-a-real-password-hash");
            userStatement.setBoolean(4, true);
            userStatement.setObject(5, java.time.OffsetDateTime.parse("2025-01-01T12:00:00Z"));
            userStatement.setObject(6, java.time.OffsetDateTime.parse("2025-01-01T12:00:00Z"));
            userStatement.executeUpdate();
            roleStatement.setObject(1, OTHER_USER_ID);
            roleStatement.executeUpdate();
            profileStatement.setObject(1, OTHER_USER_ID);
            profileStatement.setString(2, "Active Person");
            profileStatement.setString(3, "+10000000000");
            profileStatement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to seed legacy candidate migration fixture", exception);
        }

        Flyway.configure()
                .dataSource(url, username, password)
                .load()
                .migrate();
        legacySchemaPrepared = true;
    }
}
