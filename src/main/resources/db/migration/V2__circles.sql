-- Daret Phase 2: circles core (no money). Circles and their members.

CREATE TABLE circles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(120)  NOT NULL,
    contribution_amount NUMERIC(15, 2) NOT NULL CHECK (contribution_amount > 0),
    seat_count          INTEGER       NOT NULL CHECK (seat_count BETWEEN 2 AND 60),
    cycle_length_days   INTEGER       NOT NULL CHECK (cycle_length_days BETWEEN 1 AND 366),
    status              VARCHAR(16)   NOT NULL DEFAULT 'FORMING',
    sharia_mode         BOOLEAN       NOT NULL DEFAULT TRUE,
    fee_schedule_id     UUID,                       -- populated in Phase 3 (economics)
    owner_id            UUID          NOT NULL REFERENCES users (id),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT circle_status_valid
        CHECK (status IN ('DRAFT', 'FORMING', 'ACTIVE', 'COMPLETED', 'DISSOLVED'))
);

CREATE INDEX idx_circles_status ON circles (status);

CREATE TABLE circle_members (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    circle_id       UUID        NOT NULL REFERENCES circles (id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES users (id),
    slot_preference VARCHAR(16) NOT NULL DEFAULT 'FLEXIBLE',
    slot_position   INTEGER,                        -- assigned by matching at activation (Phase 5)
    standing        VARCHAR(16) NOT NULL DEFAULT 'GOOD',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_circle_member UNIQUE (circle_id, user_id),
    CONSTRAINT slot_preference_valid CHECK (slot_preference IN ('EARLY', 'FLEXIBLE', 'LATE')),
    CONSTRAINT member_standing_valid CHECK (standing IN ('GOOD', 'LATE', 'DEFAULTED')),
    CONSTRAINT slot_position_valid CHECK (slot_position IS NULL OR slot_position >= 1)
);

CREATE INDEX idx_circle_members_circle ON circle_members (circle_id);
CREATE INDEX idx_circle_members_user ON circle_members (user_id);
