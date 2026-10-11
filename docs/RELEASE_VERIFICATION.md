# Release verification

Verified October 6, 2026 against the completed local synthetic application.

## Completed checks

- Backend: 25 tests, zero failures/errors/skips, including the full intake-to-feedback journey.
- Frontend: production build succeeds; the one-step launcher starts the packaged app with embedded persistent storage. Accepted invitation state survives restart.
- Flyway: all three migrations applied on a fresh H2 database in PostgreSQL compatibility mode; JPA schema validation succeeds.
- Browser: participant login, intake consent, editable summary confirmation, assigned therapist review, matching, four-person proposal, valid schedule/fee approval, private preparation draft and participant acceptance at ₹600.
- Browser: desktop and mobile workspace layouts inspected; the mobile circle screen has no document horizontal overflow.
- Security assertions: public registration cannot create privileged roles; participants cannot act as therapists, read another person’s intake or see preparation drafts/rosters; administrators cannot retrieve private matching results.
- Workflow assertions: high/uncertain risk cannot be approved; changed intake blocks old proposals; over-budget and unavailable-time approvals fail; subsequent individual review cancels affected circles; declined invitations cannot be reaccepted to bypass another active placement.
- Privacy: own-record deletion leaves other participants’ records intact. Data export is owner-scoped.
- Google adapter: local fake HTTP responses cover valid structured output, malformed/blocked responses and failure behavior. A failed Google call never reports simulated success.

## Live deployment verification — October 10, 2026

- Backend: 30 local tests passed; the same source passed the container build in Google Cloud Build.
- Cloud Run: readiness and liveness UP, Google AI mode, demo seed accounts disabled, 0–2 instances, request-based CPU, dedicated identity. One-time bootstrap secret removed from the current revision.
- PostgreSQL: all three migrations ran on Supabase. Administrator and restricted application connections verified TLS 1.3 with CA/hostname validation. Dedicated `irene` schema; Supabase API roles have no schema access; Data API disabled.
- Live Gemini: structured generation succeeded using `gemini-3.5-flash-lite`; `gemini-embedding-001` returned 768-dimensional vectors. The full app performed real Google calls.
- 35 private HTTP workflow checks passed: health, authentication, professional review, two fictional intakes, confirmation, therapist review, matching, proposal, shared fee, invitation acceptance, preparation draft privacy, cancellation and scoped deletion.
- Public HTTPS checks passed on both Firebase domains, including SPA fallback, CORS, unauthenticated denial and admin login after bootstrap removal. The deployed coral interface was checked in the live browser.
- Billing: backend free trial confirmed; ₹500 usage alert created, excluding credits so consumption remains visible. Firebase/Gemini project has no billing account attached. This alert is not a spend cap.

See [deployment status](DEPLOYMENT_STATUS.md) for exact resources, revision and Hosting release.

## Checks still requiring external setup

Vertex/DLP and the alternative Cloud SQL profile remain untested; the deployed profile uses the Gemini Developer API and Supabase. Real Meet host/identity settings, comprehensive abuse controls, failure recovery, load testing and long-term costs remain unverified. Public-site checks and fictional workflows do not establish readiness for real patient use.

The mock AI is deterministic demonstration behavior. These tests do not establish clinical safety, matching quality, crisis detection accuracy or therapeutic effectiveness. A qualified professional must review screening and the service model before any real-user service. Repository policy permits fictional data only.

The prescribed 16-slide English pitch deck and 2:41 narrated English demo are included in docs/submission. The original local browser checks above remain useful separately from the deployed checks.

## Publication build verification — 10 October 2026

The publication checkout was built from the existing remote main branch plus the current deployed-app implementation. All 30 backend tests passed (zero failures, errors or skips), all 6 frontend role-routing tests passed, and the production frontend build succeeded. Credential-pattern scanning of the publication files found no embedded keys or tokens. The team confirmed that the legacy key present in older public history was already revoked; history was preserved.
