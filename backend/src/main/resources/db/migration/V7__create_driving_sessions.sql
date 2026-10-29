CREATE TABLE driving_sessions
(
    id             UUID          NOT NULL,
    candidate_id   UUID          NOT NULL,
    instructor_id  UUID          NOT NULL,
    vehicle_id     UUID,
    circuit_id     UUID,
    session_date   DATE          NOT NULL,
    start_time     TIME           NOT NULL,
    end_time       TIME           NOT NULL,
    price          NUMERIC(10, 3) NOT NULL,
    status         VARCHAR(20)    NOT NULL DEFAULT 'PLANNED',
    notes          TEXT,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_driving_sessions PRIMARY KEY (id),
    CONSTRAINT fk_driving_sessions_candidate FOREIGN KEY (candidate_id)
        REFERENCES candidates (id) ON DELETE RESTRICT,
    CONSTRAINT fk_driving_sessions_instructor FOREIGN KEY (instructor_id)
        REFERENCES instructors (id) ON DELETE RESTRICT,
    CONSTRAINT fk_driving_sessions_vehicle FOREIGN KEY (vehicle_id)
        REFERENCES vehicles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_driving_sessions_circuit FOREIGN KEY (circuit_id)
        REFERENCES circuits (id) ON DELETE RESTRICT,
    CONSTRAINT ck_driving_sessions_time CHECK (end_time > start_time),
    CONSTRAINT ck_driving_sessions_price CHECK (price >= 0),
    CONSTRAINT ck_driving_sessions_status CHECK (status IN ('PLANNED', 'DONE', 'CANCELLED', 'ABSENT'))
);

CREATE INDEX idx_driving_sessions_candidate_date ON driving_sessions (candidate_id, session_date);
CREATE INDEX idx_driving_sessions_instructor_date_time ON driving_sessions (instructor_id, session_date, start_time);
CREATE INDEX idx_driving_sessions_vehicle_date_time ON driving_sessions (vehicle_id, session_date, start_time);
CREATE INDEX idx_driving_sessions_status ON driving_sessions (status);
