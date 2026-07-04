-- Daret E2: AML flags (persisted case queue) + CNDP consent record on KYC.

CREATE TABLE aml_flags (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    circle_id  UUID        REFERENCES circles (id) ON DELETE SET NULL,
    type       VARCHAR(32) NOT NULL,   -- MEMBER_DEFAULT | LARGE_MOVEMENT | VELOCITY
    severity   VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',  -- LOW | MEDIUM | HIGH
    reason     TEXT        NOT NULL,
    status     VARCHAR(16) NOT NULL DEFAULT 'OPEN',    -- OPEN | CLEARED
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT aml_type_valid CHECK (type IN ('MEMBER_DEFAULT', 'LARGE_MOVEMENT', 'VELOCITY')),
    CONSTRAINT aml_severity_valid CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT aml_status_valid CHECK (status IN ('OPEN', 'CLEARED'))
);
CREATE INDEX idx_aml_status ON aml_flags (status);

-- CNDP / Loi 09-08 consent timestamp captured at KYC submission.
ALTER TABLE kyc_profiles ADD COLUMN consented_at TIMESTAMPTZ;
