CREATE TABLE payments
(
    id             UUID          NOT NULL,
    candidate_id   UUID          NOT NULL,
    amount         NUMERIC(10, 3) NOT NULL,
    payment_date   DATE          NOT NULL,
    payment_method VARCHAR(30)    NOT NULL,
    notes          TEXT,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT fk_payments_candidate FOREIGN KEY (candidate_id)
        REFERENCES candidates (id) ON DELETE RESTRICT,
    CONSTRAINT ck_payments_amount CHECK (amount > 0),
    CONSTRAINT ck_payments_method CHECK (payment_method IN ('CASH', 'CARD', 'BANK_TRANSFER', 'OTHER'))
);

CREATE INDEX idx_payments_candidate_date ON payments (candidate_id, payment_date);
