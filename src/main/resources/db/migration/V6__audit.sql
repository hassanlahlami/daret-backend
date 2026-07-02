-- Daret Phase 6: compliance. Append-only, hash-chained audit events (spec §13).

CREATE TABLE audit_events (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seq        BIGINT       NOT NULL UNIQUE,
    actor      VARCHAR(128) NOT NULL,
    action     VARCHAR(128) NOT NULL,
    target     VARCHAR(256),
    payload    TEXT         NOT NULL,
    prev_hash  VARCHAR(64)  NOT NULL,
    hash       VARCHAR(64)  NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_seq ON audit_events (seq);
