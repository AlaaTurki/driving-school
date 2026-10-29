package com.drivingschool.skills;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CandidateSkillsRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("app.jwt.secret", () -> java.util.Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationSeedsTheNineSkillsInDisplayOrder() {
        List<String> names = jdbcTemplate.queryForList(
                "select name from skills order by display_order",
                String.class
        );

        assertThat(names).containsExactly(
                "Démarrage",
                "Changement de vitesse",
                "Freinage",
                "Stationnement",
                "Créneau",
                "Marche arrière",
                "Circulation",
                "Respect du code",
                "Maîtrise du véhicule"
        );
    }

    @Test
    @Transactional
    void databaseEnforcesUniqueCandidateSkillPair() {
        UUID candidateId = insertCandidate();
        UUID skillId = firstSkillId();
        insertCandidateSkill(candidateId, skillId, 20);

        assertThatThrownBy(() -> insertCandidateSkill(candidateId, skillId, 30))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void databaseRejectsLevelOutsideZeroToOneHundred() {
        UUID candidateId = insertCandidate();

        assertThatThrownBy(() -> insertCandidateSkill(candidateId, firstSkillId(), 101))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private UUID insertCandidate() {
        UUID candidateId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into candidates (id, first_name, last_name, email, registration_date, status)
                values (?, 'Test', 'Candidate', ?, ?, 'ACTIVE')
                """, candidateId, candidateId + "@example.com", LocalDate.now());
        return candidateId;
    }

    private UUID firstSkillId() {
        return jdbcTemplate.queryForObject(
                "select id from skills order by display_order limit 1",
                UUID.class
        );
    }

    private void insertCandidateSkill(UUID candidateId, UUID skillId, int level) {
        jdbcTemplate.update("""
                insert into candidate_skills (id, candidate_id, skill_id, level)
                values (?, ?, ?, ?)
                """, UUID.randomUUID(), candidateId, skillId, level);
    }
}
