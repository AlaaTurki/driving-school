CREATE TABLE candidates
(
    id                UUID         NOT NULL,
    user_id           UUID,
    first_name        VARCHAR(120) NOT NULL,
    last_name         VARCHAR(120) NOT NULL,
    phone             VARCHAR(30)  NOT NULL DEFAULT '',
    email             VARCHAR(320) NOT NULL,
    date_of_birth     DATE,
    address           VARCHAR(500),
    registration_date DATE         NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    notes             TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_candidates PRIMARY KEY (id),
    CONSTRAINT uk_candidates_user_id UNIQUE (user_id),
    CONSTRAINT uk_candidates_email UNIQUE (email),
    CONSTRAINT fk_candidates_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_candidates_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'COMPLETED', 'SUSPENDED'))
);

INSERT INTO candidates (
    id, user_id, first_name, last_name, phone, email, registration_date, status, created_at, updated_at
)
SELECT profiles.user_id,
       users.id,
       COALESCE(NULLIF(split_part(btrim(profiles.full_name), ' ', 1), ''), split_part(users.email, '@', 1)),
       CASE
           WHEN position(' ' IN btrim(profiles.full_name)) = 0 THEN ''
           ELSE btrim(substring(btrim(profiles.full_name) FROM position(' ' IN btrim(profiles.full_name)) + 1))
       END,
       profiles.phone,
       users.email,
       users.created_at::date,
       CASE WHEN users.enabled THEN 'ACTIVE' ELSE 'INACTIVE' END,
       users.created_at,
       users.updated_at
FROM candidate_profiles profiles
JOIN users ON users.id = profiles.user_id;

DROP TABLE candidate_profiles;

CREATE INDEX idx_candidates_last_first_name ON candidates (last_name, first_name);
CREATE INDEX idx_candidates_status ON candidates (status);
