CREATE TABLE vehicles
(
    id                  UUID         NOT NULL,
    registration_number VARCHAR(30)  NOT NULL,
    brand               VARCHAR(120) NOT NULL,
    model               VARCHAR(120) NOT NULL,
    type                VARCHAR(20)  NOT NULL,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_vehicles PRIMARY KEY (id),
    CONSTRAINT uk_vehicles_registration_number UNIQUE (registration_number),
    CONSTRAINT ck_vehicles_type CHECK (type IN ('MANUAL', 'AUTOMATIC', 'MOTORCYCLE', 'TRUCK'))
);

CREATE INDEX idx_vehicles_active ON vehicles (active);

CREATE TABLE circuits
(
    id          UUID          NOT NULL,
    name        VARCHAR(160)  NOT NULL,
    description TEXT,
    location    VARCHAR(200),
    price       NUMERIC(10,3) NOT NULL,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_circuits PRIMARY KEY (id),
    CONSTRAINT uk_circuits_name UNIQUE (name),
    CONSTRAINT ck_circuits_price CHECK (price >= 0)
);

CREATE INDEX idx_circuits_active ON circuits (active);
