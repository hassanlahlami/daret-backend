# Integrations — going from mock to real partners

Daret is built around two swappable ports so the platform never handles money
or identity documents directly. Both default to **mock** adapters; switching to
a real partner is **configuration only — no business-logic change**.

| Concern | Port | Default (mock) | Real adapter | Selector |
|---|---|---|---|---|
| Payments | `PaymentProvider` | `MockPaymentProvider` | `HttpPaymentProvider` | `daret.payments.provider = mock \| http` |
| KYC | `KycProvider` | `MockKycProvider` | `HttpKycProvider` | `daret.kyc.provider = mock \| http` |

The inbound webhooks (`/api/v1/webhooks/psp`, `/api/v1/webhooks/kyc`) are already
wired and HMAC-verified — a real partner just needs the endpoint URL + secret.

## 1. Payments (PSP: CMI / PayZone / Naps / …)  — G3

1. Sign with a licensed payment institution (the platform must **not** hold
   funds — validate the regulatory posture with a Moroccan legal advisor).
2. Set env:
   ```
   DARET_PAYMENTS_PROVIDER=http
   DARET_PSP_BASE_URL=https://sandbox.psp.example
   DARET_PSP_API_KEY=...            # from the PSP
   DARET_PSP_WEBHOOK_SECRET=...     # shared HMAC secret
   # optional path overrides:
   DARET_PSP_COLLECT_PATH=/charges
   DARET_PSP_DISBURSE_PATH=/transfers
   DARET_PSP_STATUS_PATH=/transactions/{ref}
   ```
3. In `HttpPaymentProvider`, adapt `MovementRequest` / `MovementResponse` and the
   paths to the PSP's contract, and map its status vocabulary in `mapStatus`.
4. Point the PSP's webhook at `POST /api/v1/webhooks/psp` (body `{externalRef,status}`,
   header `X-PSP-Signature` = HMAC-SHA256 of the raw body).
5. Test in **sandbox** first (settlements reconcile via webhook + the poll reconciler).

## 2. KYC (eKYC partner)  — G4

1. Sign with an eKYC provider (declare the processing with the **CNDP**, loi 09-08).
2. Set env:
   ```
   DARET_KYC_PROVIDER=http
   DARET_KYC_BASE_URL=https://sandbox.ekyc.example
   DARET_KYC_API_KEY=...
   DARET_KYC_INITIATE_PATH=/sessions
   DARET_PSP_WEBHOOK_SECRET=...     # reused for the KYC webhook signature
   ```
3. In `HttpKycProvider`, adapt the request/`SessionResponse` shape to the partner.
4. Flow: client calls `POST /api/v1/kyc/initiate` → gets `{partnerRef, redirectUrl}`
   → user completes verification at the partner → partner calls
   `POST /api/v1/webhooks/kyc` (`{userId, decision}`) → profile becomes VERIFIED/REJECTED.

## Notes
- Nothing else in the codebase references the concrete adapters — callers depend
  only on the ports, so swapping is safe.
- Keep secrets in a secret manager (Vault), never in the repo (see G1/G2).
