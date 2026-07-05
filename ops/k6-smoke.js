// Daret — k6 smoke / light load test (F3).
//
// Exercises the critical read/auth paths against a running backend and asserts
// latency + error-rate thresholds. This is a SMOKE test (a few VUs, ~30s), not
// a capacity benchmark — it proves the app stays healthy under concurrent load.
//
// Run against a seeded backend (see README "verify" loop):
//   BASE_URL=http://localhost:8080 k6 run ops/k6-smoke.js
//
// Optional env:
//   BASE_URL  target base (default http://localhost:8080)
//   VUS       virtual users (default 5)
//   DURATION  test duration (default 30s)

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: Number(__ENV.VUS || 5),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],      // <1% errors
    http_req_duration: ['p(95)<800'],    // 95% of requests under 800ms
    checks: ['rate>0.99'],               // >99% of checks pass
  },
};

function unique() {
  return `k6_${Date.now()}_${Math.floor(Math.random() * 1e6)}@smoke.ma`;
}

export default function () {
  // 1. Liveness + readiness probes.
  const live = http.get(`${BASE}/actuator/health/liveness`);
  check(live, { 'liveness UP': (r) => r.status === 200 });

  const ready = http.get(`${BASE}/actuator/health/readiness`);
  check(ready, { 'readiness UP': (r) => r.status === 200 });

  // 2. Register a fresh member (auth path + write).
  const email = unique();
  const reg = http.post(`${BASE}/api/v1/auth/register`,
    JSON.stringify({ email, password: 'password123' }),
    { headers: { 'Content-Type': 'application/json' } });
  check(reg, { 'register 200': (r) => r.status === 200 });
  const token = reg.status === 200 ? reg.json('accessToken') : null;

  if (token) {
    const authHeaders = { headers: { Authorization: `Bearer ${token}` } };

    // 3. Authenticated reads.
    const circles = http.get(`${BASE}/api/v1/circles`, authHeaders);
    check(circles, { 'circles 200': (r) => r.status === 200 });

    const score = http.get(`${BASE}/api/v1/me/score`, authHeaders);
    check(score, { 'score 200': (r) => r.status === 200 });

    // 4. CNDP data export (F1) — a heavier aggregate read.
    const exp = http.get(`${BASE}/api/v1/me/data-export`, authHeaders);
    check(exp, { 'data-export 200': (r) => r.status === 200 });
  }

  sleep(1);
}
