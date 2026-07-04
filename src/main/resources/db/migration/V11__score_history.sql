-- Daret: credit-score history — a snapshot each time a user's score changes.

CREATE TABLE score_snapshots (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    score      INTEGER     NOT NULL,
    tier       VARCHAR(1)  NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_score_snapshots_user ON score_snapshots (user_id, created_at DESC);
