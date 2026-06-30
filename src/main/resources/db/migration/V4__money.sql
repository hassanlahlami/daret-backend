-- Daret Phase 4: money (mocked). Slots, contributions, payouts, payment intents,
-- the double-entry ledger, the transactional outbox, and the ShedLock table.

-- ---------------------------------------------------------------------------
-- Priced slots (created at activation)
-- ---------------------------------------------------------------------------
CREATE TABLE slots (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    circle_id       UUID        NOT NULL REFERENCES circles (id) ON DELETE CASCADE,
    position        INTEGER     NOT NULL CHECK (position >= 1),
    position_fee    NUMERIC(15, 2) NOT NULL DEFAULT 0,
    payout_bonus    NUMERIC(15, 2) NOT NULL DEFAULT 0,
    platform_funded BOOLEAN     NOT NULL DEFAULT FALSE,
    user_id         UUID        NOT NULL REFERENCES users (id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_slot_position UNIQUE (circle_id, position)
);
CREATE INDEX idx_slots_circle ON slots (circle_id);

-- ---------------------------------------------------------------------------
-- Contributions (one per member per cycle)
-- ---------------------------------------------------------------------------
CREATE TABLE contributions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    circle_member_id  UUID        NOT NULL REFERENCES circle_members (id) ON DELETE CASCADE,
    circle_id         UUID        NOT NULL REFERENCES circles (id) ON DELETE CASCADE,
    user_id           UUID        NOT NULL REFERENCES users (id),
    cycle_index       INTEGER     NOT NULL CHECK (cycle_index >= 1),
    amount            NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    due_date          DATE        NOT NULL,
    status            VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED',
    payment_intent_id UUID,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_contribution UNIQUE (circle_member_id, cycle_index),
    CONSTRAINT contribution_status_valid CHECK (status IN ('SCHEDULED', 'PAID', 'MISSED'))
);
CREATE INDEX idx_contributions_circle ON contributions (circle_id);
CREATE INDEX idx_contributions_status_due ON contributions (status, due_date);

-- ---------------------------------------------------------------------------
-- Payouts (exactly one per member per circle)
-- ---------------------------------------------------------------------------
CREATE TABLE payouts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    circle_id           UUID        NOT NULL REFERENCES circles (id) ON DELETE CASCADE,
    recipient_member_id UUID        NOT NULL REFERENCES circle_members (id) ON DELETE CASCADE,
    recipient_user_id   UUID        NOT NULL REFERENCES users (id),
    cycle_index         INTEGER     NOT NULL CHECK (cycle_index >= 1),
    amount              NUMERIC(15, 2) NOT NULL,
    platform_cut        NUMERIC(15, 2) NOT NULL DEFAULT 0,
    status              VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED',
    payment_intent_id   UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_payout_member UNIQUE (circle_id, recipient_member_id),
    CONSTRAINT uq_payout_cycle UNIQUE (circle_id, cycle_index),
    CONSTRAINT payout_status_valid CHECK (status IN ('SCHEDULED', 'PAID', 'FAILED'))
);

-- ---------------------------------------------------------------------------
-- Payment intents (the only thing touching money)
-- ---------------------------------------------------------------------------
CREATE TABLE payment_intents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    direction       VARCHAR(16) NOT NULL,
    amount          NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    external_ref    VARCHAR(128),
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    ref_type        VARCHAR(32) NOT NULL,
    ref_id          UUID        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT payment_direction_valid CHECK (direction IN ('COLLECT', 'DISBURSE')),
    CONSTRAINT payment_status_valid CHECK (status IN ('PENDING', 'SETTLED', 'FAILED'))
);

-- ---------------------------------------------------------------------------
-- Double-entry ledger (append-only; balances derived, never stored)
-- ---------------------------------------------------------------------------
CREATE TABLE ledger_entries (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tx_id       UUID        NOT NULL,
    account     VARCHAR(128) NOT NULL,
    debit       NUMERIC(15, 2) NOT NULL DEFAULT 0 CHECK (debit >= 0),
    credit      NUMERIC(15, 2) NOT NULL DEFAULT 0 CHECK (credit >= 0),
    ref_type    VARCHAR(32) NOT NULL,
    ref_id      UUID        NOT NULL,
    cycle_index INTEGER,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ledger_account ON ledger_entries (account);
CREATE INDEX idx_ledger_tx ON ledger_entries (tx_id);

-- ---------------------------------------------------------------------------
-- Transactional outbox
-- ---------------------------------------------------------------------------
CREATE TABLE outbox_events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type         VARCHAR(64) NOT NULL,
    payload      TEXT        NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts     INTEGER     NOT NULL DEFAULT 0,
    processed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT outbox_status_valid CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED'))
);
CREATE INDEX idx_outbox_status ON outbox_events (status, created_at);

-- ---------------------------------------------------------------------------
-- ShedLock (single-instance scheduled jobs)
-- ---------------------------------------------------------------------------
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMPTZ  NOT NULL,
    locked_at  TIMESTAMPTZ  NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
