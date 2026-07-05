# Changelog — Daret Backend

All notable changes to the Daret backend. Versioning is
[SemVer](https://semver.org/); dates are ISO-8601.

## [1.0.0-rc] — 2026-07-05

First release candidate. The platform is feature-complete for the MVP scope
plus the post-MVP extension set (E1–E8) and the regulatory/release/reliability
closeout (F1–F3). Money remains **mocked behind the `PaymentProvider` port** —
the platform never touches real funds.

### Phases 1–7 (MVP)
- **Identity & KYC** — email/phone auth, BCrypt, JWT with one-shot refresh,
  roles (MEMBER / COMPLIANCE_OFFICER / ADMIN), KYC gate.
- **Circles** — create/join/discover, seat preferences, state machine
  (DRAFT → FORMING → ACTIVE → COMPLETED / DISSOLVED), activation.
- **Economics** — `FeeEngine` curve, fee schedules, fee preview, margin
  invariant.
- **Money (mocked)** — double-entry ledger with derived balances, transactional
  outbox, sign-aware platform cut, activation flow.
- **Risk & matching** — pluggable scoring (fresh verified = tier B), score gate,
  gap-fill exposure cap, platform system user, default sweep.
- **Compliance** — `@Audited` AOP → SHA-256 hash-chained audit events, `@Pii`
  masking, AML heuristics, KYC review console.

### Extensions (E1–E8, E3-lite)
- **E1** — async settlement: HMAC webhook (`/webhooks/psp`) + poll reconciler,
  idempotent; scheduled via ShedLock.
- **E2** — AML rule engine (`AmlFlag`), KYC webhook, CNDP consent on KYC submit.
- **E4** — trust & social: vouching (score signal, +25/vouch capped +150) and
  invite-only circles (`DRT-XXXXXXXX`).
- **E5** — persistent notification center (in-app inbox for every emit).
- **E6 / E6b** — pay-now (early payment) and member dispute/grievance flow.
- **E7** — admin & ops console (`/admin/overview`, circles, users, ops).
- **E8** — admin analytics (`/admin/analytics`, weekly buckets).
- **E3-lite** — login lockout (423 after N fails) + security headers (CSP, HSTS,
  X-Frame-Options, nosniff, Referrer-Policy).
- Score history snapshots (`/me/score/history`).

### Release closeout (F1–F3)
- **F1** — CNDP data rights: `GET /me/data-export` (right of access, un-masked
  own-data bundle, audited) and `POST /me/erasure-request` (right to erasure,
  reviewable by compliance). Migration `V12`.
- **F2** — release engineering: OpenAPI spec bumped to `1.0.0-rc` (springdoc at
  `/v3/api-docs`, Swagger UI at `/swagger-ui.html`), version bump, this
  changelog, `v1.0.0-rc` tag.
- **F3** — reliability: actuator liveness/readiness probe groups, graceful
  shutdown, k6 smoke script (`ops/k6-smoke.js`).

### Verification
- 23 unit tests (Surefire, no Docker) + Testcontainers integration tests
  (Failsafe, `mvn verify`).
- Migrations at **V12**.
