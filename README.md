<div align="center">

# 🔄 Daret — Backend

### Plateforme d'épargne tournante (ROSCA) — API & moteur

Monolithe modulaire Spring Boot : identité/KYC, cercles, économie (frais), scoring &
matching, **grand livre en partie double**, risque/gap-fill, et **conformité inviolable**.
La plateforme ne détient **jamais** les fonds — tout passe par un port `PaymentProvider`.

![CI](https://github.com/hassanlahlami/daret-backend/actions/workflows/ci.yml/badge.svg)
&nbsp;![Java](https://img.shields.io/badge/Java-21-orange)
&nbsp;![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F)
&nbsp;![Postgres](https://img.shields.io/badge/PostgreSQL-16-336791)
&nbsp;![Tests](https://img.shields.io/badge/tests-23%20unit%20%2B%20IT-brightgreen)
&nbsp;![Release](https://img.shields.io/badge/release-1.0.0--rc-blue)

</div>

> App web (PWA Next.js, fr/ar/darija) : **[hassanlahlami/daret-frontend](https://github.com/hassanlahlami/daret-frontend)**.
> Spécification complète : [`daret-platform-spec.md`](./daret-platform-spec.md).

## 🇬🇧 In English

**Daret is a fintech backend for group savings circles (ROSCA), built as a modular Spring Boot
monolith on Java 21 and PostgreSQL.** It is a complete, tested reference for the parts of a money
product that are easy to get wrong:

- **Double-entry ledger** with derived balances, so every movement can be audited and nothing is a
  free-standing number.
- **Payments behind a provider port.** The platform never holds funds. Settlement is asynchronous:
  an outbox sends intents to the payment provider, **HMAC-signed, idempotent webhooks** confirm them,
  and a scheduled reconciler (ShedLock) catches anything missed.
- **Risk and compliance built in:** KYC gate, exposure cap on gap-fill, a **SHA-256 hash-chained audit
  trail** (AOP `@Audited`), and PII masking.
- **Production habits:** Flyway migrations, JWT with rotating refresh tokens, rate limiting, health
  probes and Prometheus metrics, Docker Compose for the full stack, CI, unit and Testcontainers
  integration tests, Swagger UI.

If you need a payments, ledger or banking-API integration built carefully, this is the kind of work I
do: [Upwork profile](https://www.upwork.com/freelancers/~015cce5800215d394a).

## 🚀 Démarrer

**Full stack** (clonez les deux dépôts côte à côte sous un même parent) :

```bash
# parent/
#   daret-backend/   (ce dépôt, porte le docker-compose)
#   daret-frontend/
cp .env.example .env
docker compose up --build
```

- Frontend → http://localhost:3000 · API → http://localhost:8080/api/v1/health
- Swagger → http://localhost:8080/swagger-ui.html · Adminer → http://localhost:8081
- Données de démo : `DARET_SEED_ENABLED=true` (voir le [scénario guidé](https://github.com/hassanlahlami/daret-frontend/blob/main/docs/DEMO.md)).

**Backend seul** (Postgres sur `:5432`) :

```bash
mvn spring-boot:run
```

## 🧩 Modules (`ma.daret`)

```
common/        base entity, enveloppe d'erreur (+403), health, masquage PII
config/        sécurité, CORS, OpenAPI, rate limit, scheduler (ShedLock), props risk/payments
identity/      auth (JWT + refresh rotatif), user (+ compte système plateforme), kyc (+ KycGate)
circles/       Circle/CircleMember/Slot/Contribution/Payout, state machine, activation, schedules
economics/     FeeSchedule, FeeEngine (courbe de frais de position), aperçu de frais
scoring/       CreditScore, ScoringStrategy enfichable, /me/score
risk/          MatchingService (score gate), RiskService (plafond gap-fill), sweep de défaut
ledger/        LedgerEntry en partie double + LedgerService (soldes dérivés)
payments/      port PaymentProvider + MockPaymentProvider, intents, outbox, pipeline, ops
compliance/    aspect AOP @Audited, AuditEvent haché, console conformité
notifications/ NotificationService agnostique de canal
seed/          seeder de démo
```

Migrations Flyway : `V1..V6`. Schéma possédé par Flyway ; Hibernate en `validate`.

## 💳 Paiements (règlement asynchrone)

Tout passe par le port `PaymentProvider` (`MockPaymentProvider` en MVP ; adaptateur
PSP HTTP en prod). Le règlement réel est **asynchrone** :

- l'outbox envoie l'intent au PSP → statut `PENDING` ;
- le PSP confirme via **webhook signé HMAC** (`POST /api/v1/webhooks/psp`) — vérifié,
  idempotent → règle la contribution/le versement + écriture au grand livre ;
- un **reconciler planifié** (ShedLock) interroge (`poll`) les intents en attente en
  filet de sécurité ; déclenchable à la demande via `POST /admin/ops/reconcile`.

## 🔭 Observabilité

- `/actuator/health` (+ sondes `liveness`/`readiness`) · `/actuator/prometheus` (Micrometer) · `/actuator/info` (version de build).

## ✅ Tests

```bash
mvn test      # tests unitaires (Surefire) — sans Docker
mvn verify    # + tests d'intégration (Failsafe + Testcontainers) ; ignorés si Docker indisponible
```

## 🔒 Invariants (appliqués dans le code)

- **Aucune détention de fonds** : l'argent ne croise que le port `PaymentProvider` ; les
  comptes du grand livre modélisent des créances, jamais un solde dépensable (soldes dérivés).
- **Jamais d'« intérêt »** : champs `serviceFee` / `positionFee` / `payoutBonus`.
- **Gate KYC**, **plafond d'exposition** gap-fill, **chaîne d'audit** SHA-256, **PII** masquées (CNDP).
- Secrets par env — jamais commiter `.env` ; `openssl rand -base64 48` pour `DARET_JWT_SECRET` ;
  restreindre `/actuator/prometheus` au réseau de supervision en prod.
