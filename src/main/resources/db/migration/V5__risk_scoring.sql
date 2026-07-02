-- Daret Phase 5: risk & matching. Credit scores + the platform gap-fill account.

CREATE TABLE credit_scores (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    score       INTEGER     NOT NULL CHECK (score BETWEEN 0 AND 1000),
    tier        VARCHAR(1)  NOT NULL,
    factors     JSONB       NOT NULL DEFAULT '{}'::jsonb,
    computed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT credit_tier_valid CHECK (tier IN ('A', 'B', 'C', 'D', 'E'))
);

-- ---------------------------------------------------------------------------
-- Platform system user: the sole occupant of gap-filled slots (spec §5.4).
-- Login-disabled; tier A so it can back any slot within the exposure cap.
-- ---------------------------------------------------------------------------
INSERT INTO users (id, email, password_hash, enabled)
VALUES ('00000000-0000-0000-0000-0000000000f1', 'platform@daret.system', 'disabled', FALSE);

INSERT INTO kyc_profiles (user_id, status)
VALUES ('00000000-0000-0000-0000-0000000000f1', 'VERIFIED');

INSERT INTO credit_scores (user_id, score, tier, factors)
VALUES ('00000000-0000-0000-0000-0000000000f1', 1000, 'A', '{"system": 1000}'::jsonb);

-- The platform may hold several gap-fill seats in one circle, so relax the
-- (circle_id, user_id) uniqueness to a partial index that still guards real users.
ALTER TABLE circle_members DROP CONSTRAINT uq_circle_member;
CREATE UNIQUE INDEX uq_circle_member_real ON circle_members (circle_id, user_id)
    WHERE user_id <> '00000000-0000-0000-0000-0000000000f1';
