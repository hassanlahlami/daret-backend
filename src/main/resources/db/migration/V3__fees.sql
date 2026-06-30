-- Daret Phase 3: economics. Fee schedules drive the position-fee curve (spec §5.3).
-- Amounts in MAD. No "interest" anywhere — serviceFee / positionFee / payoutBonus only.

CREATE TABLE fee_schedules (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(80)   NOT NULL UNIQUE,
    service_fee          NUMERIC(15, 2) NOT NULL CHECK (service_fee >= 0),
    base_fee             NUMERIC(15, 2) NOT NULL CHECK (base_fee >= 0),
    redistribution_share NUMERIC(15, 2) NOT NULL CHECK (redistribution_share >= 0),
    tier_multipliers     JSONB         NOT NULL DEFAULT '{}'::jsonb,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Platform default schedule (referenced by FeeScheduleRepository.DEFAULT_ID).
-- Invariant: base_fee * min(multiplier) >= redistribution_share  →  50 * 1.0 >= 40.
INSERT INTO fee_schedules (id, name, service_fee, base_fee, redistribution_share, tier_multipliers)
VALUES (
    '00000000-0000-0000-0000-0000000000d1',
    'DEFAULT',
    25.00, 50.00, 40.00,
    '{"A": 1.0, "B": 1.15, "C": 1.35, "D": 1.6, "E": 2.0}'::jsonb
);

-- Now that a default exists, tie circles to their schedule.
ALTER TABLE circles
    ADD CONSTRAINT fk_circles_fee_schedule
        FOREIGN KEY (fee_schedule_id) REFERENCES fee_schedules (id);
