CREATE TABLE instructors
(
    id             UUID         NOT NULL,
    user_id        UUID         NOT NULL,
    first_name     VARCHAR(120) NOT NULL,
    last_name      VARCHAR(120) NOT NULL,
    phone          VARCHAR(30)  NOT NULL,
    email          VARCHAR(320) NOT NULL,
    license_number VARCHAR(100) NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_instructors PRIMARY KEY (id),
    CONSTRAINT uk_instructors_user_id UNIQUE (user_id),
    CONSTRAINT uk_instructors_email UNIQUE (email),
    CONSTRAINT uk_instructors_license_number UNIQUE (license_number),
    CONSTRAINT fk_instructors_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_instructors_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_instructors_last_first_name ON instructors (last_name, first_name);
CREATE INDEX idx_instructors_status ON instructors (status);
