-- Daret E4: trust & social layer — vouches (peer trust) + invite-only circles.

-- A member vouches for another → a positive credit-scoring signal.
CREATE TABLE vouches (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    voucher_user_id  UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    vouchee_user_id  UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_vouch UNIQUE (voucher_user_id, vouchee_user_id),
    CONSTRAINT vouch_not_self CHECK (voucher_user_id <> vouchee_user_id)
);
CREATE INDEX idx_vouches_vouchee ON vouches (vouchee_user_id);

-- Private circles: joining requires a valid invite code.
ALTER TABLE circles ADD COLUMN invite_only BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE circle_invites (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(32) NOT NULL UNIQUE,
    circle_id       UUID        NOT NULL REFERENCES circles (id) ON DELETE CASCADE,
    inviter_user_id UUID        NOT NULL REFERENCES users (id),
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | USED | REVOKED
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT invite_status_valid CHECK (status IN ('ACTIVE', 'USED', 'REVOKED'))
);
CREATE INDEX idx_invites_circle ON circle_invites (circle_id);
