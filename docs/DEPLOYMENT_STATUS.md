# Irene deployment status

Deployment verified 10 October 2026; handoff updated and public availability rechecked 12 October 2026. This is a working prototype for **fictional data only**, not a clinical service.

## Open the app

- **Website:** https://project-irene-fd39e.web.app
- Alternate website: https://project-irene-fd39e.firebaseapp.com
- Backend: https://irene-api-oz6fksw45q-el.a.run.app
- Private administrator and fictional demo logins: `deploy/hosted/local-credentials.json` on this computer. This file is ignored by Git, Docker and Cloud Build. Do not share it publicly or include it in a submission.

Select Participant, Therapist or Admin on the sign-in card, then use the matching private credentials. Unverified accounts choosing Therapist open a professional application; approval is still required for therapist privileges. The facilitator account is a clearly labelled fictional test professional. Two fictional participant accounts are available. One participant's intake was deleted to verify privacy controls; the other has a reviewed intake. The test circle was cancelled, so no real appointment or meeting was created. The public program is explicitly labelled “Fictional Demo: College Connection.” Create fresh fictional accounts to record a clean demo, and have the administrator verify any new fictional professional before using its workspace.

## Resources actually created

| Component | Deployed resource |
| --- | --- |
| Firebase Hosting + Gemini | Project `project-irene-fd39e` (number `978735791945`), unlinked from billing; Firebase Spark |
| Backend Google project | `project-5933e959-60ec-4cc4-be3` (number `776197325064`), Google free trial |
| Cloud Run | `irene-api`, `asia-south1`; minimum 0, maximum 2, 1 CPU, 1 GiB, concurrency 10 |
| Runtime identity | `irene-runtime@project-5933e959-60ec-4cc4-be3.iam.gserviceaccount.com` |
| Build identity | `irene-builder@project-5933e959-60ec-4cc4-be3.iam.gserviceaccount.com` |
| Container registry | `asia-south1-docker.pkg.dev/project-5933e959-60ec-4cc4-be3/irene` |
| Build source bucket | `gs://project-5933e959-60ec-4cc4-be3-irene-build-source` (private) |
| Supabase | Free project `envjofelxhlrucwxewse`, Mumbai; Data API disabled |
| Database connection | Session pooler `aws-0-ap-south-1.pooler.supabase.com:5432`, database `postgres`, role `irene_app`, schema `irene` |
| AI | Live `gemini-3.5-flash-lite` generation and `gemini-embedding-001` embeddings; API key restricted to the Generative Language API |

The database connection uses certificate-verified TLS and a dedicated restricted role. The app never receives the Supabase administrator password. Each backend instance allows at most five database connections and zero idle connections. All three Flyway migrations succeeded.

## Release and verification

- Current Cloud Run revision: **`irene-api-00002-j24`**.
- Successful Cloud Build: `2c79d422-aad8-4ced-8f50-e73d92f58691`.
- Immutable backend image: `asia-south1-docker.pkg.dev/project-5933e959-60ec-4cc4-be3/irene/backend@sha256:dca2636882d7d75b6dc020b60631a6f08b8d755e8d4aafc936a634661417966c`.
- Current Hosting version: **`afda15c769e5aac4`**.
- Hosting release: `sites/project-irene-fd39e/releases/1791636230375000`.
- All 30 backend tests passed locally; the Cloud Build image also ran Maven verification successfully.
- Production frontend build passed and its final sign-in text was checked in the live browser.
- 35 live workflow checks passed using fictional accounts, including intake, real AI generation/embeddings, human review, matching, group approval, shared pricing, invitation acceptance, private session preparation, cancellation and intake deletion.
- Readiness/liveness passed; authenticated administration remained functional after removing bootstrap settings. Unauthenticated workspaces were denied. Allowed-origin CORS, rejected foreign origins, hidden API docs, HTTPS, nested SPA routing and security headers were checked.

This does not validate clinical suitability, crisis detection accuracy, abuse resistance under public traffic, or high-load performance. No real care, payments, messages, or Google Meet sessions were delivered by these checks.

## Secrets and access

Four enabled runtime secrets remain in the backend Google project: `irene-db-password`, `irene-jwt-secret`, `irene-gemini-key`, and `irene-supabase-ca`. The CA certificate is mounted read-only. The runtime service account has access only to its named runtime secrets; the build account has no application-secret access. No service-account key file was downloaded.

Both one-time secret versions, `irene-db-setup-password/versions/1` and `irene-bootstrap-password/versions/1`, are **disabled**. Bootstrap settings and bootstrap secret access were removed from the running service. The administrator can still sign in with its existing hashed database credential. Keep the original Supabase password in your password manager.

Cloud Run permits public invocation so browsers can reach the API; application authentication and role checks still protect private endpoints. CORS permits exactly the two Firebase origins. There are no seeded accounts with a shared public demo password.

## Costs and availability

Firebase/Gemini remain unbilled, and Supabase remains Free. The backend account was not upgraded to paid billing. At setup, Google displayed ₹28,796.63 trial credit and 90 days remaining. Inspect your Billing page for the current balance and end date.

A ₹500 monthly project budget alert was created, with 50%, 90% and 100% notifications and credits excluded. **This is an alert, not a spending cap.** Minimum zero and maximum two reduce exposure but do not cap requests, storage, builds, network or total cost. For a light demo, usage may stay within allowances; zero cost is not guaranteed. See the linked official pricing and estimates in [the setup guide](GOOGLE_CLOUD_SETUP.md#2-expected-costs-and-billing-risks).

Cold starts can make the first request slow. Supabase Free may pause after a week of inactivity; open its dashboard and restore the project before presenting if needed. No automatic database backup has been configured. Free AI quotas may interrupt requests. Check the app and Google/Supabase dashboards before the presentation.

## Future updates and rollback

Use [the beginner setup/deployment guide](GOOGLE_CLOUD_SETUP.md) for commands and console steps. Do not recreate the listed resources. Local `deploy/hosted/env.yaml` contains the deployed non-secret values. The frontend needs the backend HTTPS URL when built.

The official Google CLI used for this release is under `/private/tmp/irene-google-tools`, with a private temporary login configuration. That location is not a permanent installation and may disappear. Install/sign in to the official Google Cloud CLI for later work, then explicitly select the backend project. Firebase Hosting was published using its official REST API with the authorized Google login; a persistent Firebase CLI login was not configured. Run `firebase login` and target `project-irene-fd39e` explicitly for future CLI publishing.

Before a backend update, create a database backup and review migrations. Build/test the new image, then deploy and verify. The repository deployment script starts the service privately; deliberately restore public invocation only after checking authentication and health, as described in the guide. Do not put bootstrap settings back into normal releases.

To restore this known-good backend after a future change:

```sh
gcloud run services update-traffic irene-api \
  --project=project-5933e959-60ec-4cc4-be3 \
  --region=asia-south1 \
  --to-revisions=irene-api-00002-j24=100
```

Do not route traffic to the earlier bootstrap revision `irene-api-00001-ng7`; its one-time secret is disabled. If the baseline revision is no longer available, redeploy the immutable image above using the four runtime secrets and current environment file, without bootstrap variables.

For the frontend, open **Firebase Console → Project Irene → Hosting → Release history**, select the previous verified release and choose **Roll back**. The first verified Hosting version was `e79140621765cc31`; it predates the role selector and landing-page copy refinements. Prefer the current version recorded above as the baseline for subsequent releases.

Rolling back application traffic does not roll back database migrations or restore deleted records. Restore a reviewed database backup separately when needed. No baseline database export was created during this deployment.

To temporarily stop public API access while retaining data, remove the service's `allUsers` / Cloud Run Invoker binding; existing in-flight requests may finish. Storage/build resources can still have costs. To retire the demo completely, review and remove only Irene resources after saving the data you want. Do not delete unrelated projects or the user's billing account.

## Project handoff

Deployment and publication are complete. The tested source and prescribed-template deck are in the [public repository](https://github.com/TheAverageGatsby13/project-irene). The final [2:41 Aoede-narrated video](https://drive.google.com/file/d/1VxKqMv-1Tg9g7m6cUbuaK8eWOmsG7w3e/view) was uploaded to Google Drive on 12 October 2026 with anyone-with-link viewing enabled; its link is included in both deck formats. The website returned HTTP 200 and the API health endpoint reported `status=ok`, `aiMode=google`, and `demoAccounts=false` during the 12 October availability check. These checks do not repeat the full workflow evaluation. The team still needs to submit the materials through the organizer dashboard; no submission confirmation has been recorded. Keep credentials and real health information out of submission materials.

### Sign-in UI refinement, 10 October 2026

Published explicit Participant / Therapist / Admin entry points, a direct professional application route for unverified users, inline authentication errors, and an explanation of AI assistance in the landing journey. Removed the requested top status banner and footer clause. Intake consent and backend authorization remain intact. Six frontend routing tests and the production build passed. Therapist application and admin sign-in states were checked in the browser. The backend image, Cloud Run revision, database and secrets were unchanged. Previous Hosting version `75b221246f80ffbe` remains the immediate rollback point.
