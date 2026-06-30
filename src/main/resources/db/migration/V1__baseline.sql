-- Daret baseline schema (Phase 1: identity — users, roles, KYC, refresh tokens).
-- UUID primary keys; timestamps in UTC. Later phases add circles, ledger, audit, etc.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ---------------------------------------------------------------------------
-- Users & roles
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE,
    phone         VARCHAR(32)  UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- A user must be reachable by at least one identifier.
    CONSTRAINT users_contact_present CHECK (email IS NOT NULL OR phone IS NOT NULL)
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- ---------------------------------------------------------------------------
-- KYC profiles
--   Raw identity documents are NEVER stored — only an opaque partner reference.
-- ---------------------------------------------------------------------------
CREATE TABLE kyc_profiles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    status      VARCHAR(16) NOT NULL DEFAULT 'PENDING',  -- PENDING | VERIFIED | REJECTED
    partner_ref VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT kyc_status_valid CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

-- ---------------------------------------------------------------------------
-- Refresh tokens (rotating). Only a hash of the token is stored.
-- ---------------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,  -- SHA-256 hex of the opaque token
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
