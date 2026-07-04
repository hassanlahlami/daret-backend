-- Daret E6b: member dispute / grievance flow, surfaced to compliance.

CREATE TABLE disputes (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    raiser_user_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    circle_id      UUID        REFERENCES circles (id) ON DELETE SET NULL,
    subject        VARCHAR(160) NOT NULL,
    description    TEXT        NOT NULL,
    status         VARCHAR(16) NOT NULL DEFAULT 'OPEN',   -- OPEN | RESOLVED
    resolution     TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT dispute_status_valid CHECK (status IN ('OPEN', 'RESOLVED'))
);
CREATE INDEX idx_disputes_status ON disputes (status, created_at DESC);
CREATE INDEX idx_disputes_raiser ON disputes (raiser_user_id);
