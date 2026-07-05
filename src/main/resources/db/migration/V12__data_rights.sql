-- Daret F1: CNDP data rights. Right-of-access is served live (no table needed);
-- right-to-erasure is a reviewable request queued for a compliance officer, since
-- money/ledger records cannot be hard-deleted while obligations remain.

CREATE TABLE erasure_requests (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    reason      TEXT,
    status      VARCHAR(16) NOT NULL DEFAULT 'PENDING',  -- PENDING | APPROVED | REJECTED
    resolution  TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT erasure_status_valid CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);
CREATE INDEX idx_erasure_status ON erasure_requests (status, created_at DESC);
CREATE INDEX idx_erasure_user ON erasure_requests (user_id);
