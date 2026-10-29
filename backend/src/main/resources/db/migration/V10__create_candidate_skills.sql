CREATE TABLE skills
(
    id            UUID         NOT NULL,
    name          VARCHAR(120) NOT NULL,
    display_order INT          NOT NULL,
    CONSTRAINT pk_skills PRIMARY KEY (id),
    CONSTRAINT uk_skills_name UNIQUE (name),
    CONSTRAINT uk_skills_display_order UNIQUE (display_order)
);

CREATE TABLE candidate_skills
(
    id           UUID        NOT NULL,
    candidate_id UUID        NOT NULL,
    skill_id     UUID        NOT NULL,
    level        INT         NOT NULL DEFAULT 0,
    updated_by   UUID,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_candidate_skills PRIMARY KEY (id),
    CONSTRAINT fk_candidate_skills_candidate FOREIGN KEY (candidate_id)
        REFERENCES candidates (id) ON DELETE CASCADE,
    CONSTRAINT fk_candidate_skills_skill FOREIGN KEY (skill_id)
        REFERENCES skills (id) ON DELETE RESTRICT,
    CONSTRAINT fk_candidate_skills_updated_by FOREIGN KEY (updated_by)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT uk_candidate_skills_candidate_skill UNIQUE (candidate_id, skill_id),
    CONSTRAINT ck_candidate_skills_level CHECK (level BETWEEN 0 AND 100)
);

INSERT INTO skills (id, name, display_order)
VALUES (gen_random_uuid(), 'Démarrage', 1),
       (gen_random_uuid(), 'Changement de vitesse', 2),
       (gen_random_uuid(), 'Freinage', 3),
       (gen_random_uuid(), 'Stationnement', 4),
       (gen_random_uuid(), 'Créneau', 5),
       (gen_random_uuid(), 'Marche arrière', 6),
       (gen_random_uuid(), 'Circulation', 7),
       (gen_random_uuid(), 'Respect du code', 8),
       (gen_random_uuid(), 'Maîtrise du véhicule', 9);
