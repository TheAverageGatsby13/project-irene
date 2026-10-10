# Irene product build plan

Prepared 6 October 2026. This plan records competition requirements and the remaining validation/submission work. The core application journey is now implemented. Build one complete, trustworthy journey: private intake, participant-confirmed summary, therapist-reviewed group proposal, invitation, and session preparation. Demo only with fictional adults.

## Competition requirements

The closest published theme is **Sustainability & Social Impact**. Google AI plus Google Cloud hosting is required. The challenge page also lists Healthcare and Social Good under a separate category field; use the exact dashboard choices when submitting. Scoring is technical/GenAI 40%, impact 25%, innovation 25%, UX 10%. Submit an English PDF deck with architecture and business case, a working live URL, public source repository, and a video **strictly under three minutes**. [Requirements](https://aibuildercup.com/themes.html), [FAQ](https://aibuildercup.com/Faqs.html).

Team formation closes 11 October; submission is 18 October. Aim to finish on 17 October. The public pages do not establish the precise cutoff timezone. Finalists are announced 7 November; the Singapore finale is 4 December. [Schedule](https://aibuildercup.com/).

The terms require fresh code/assets and exclude ongoing projects. The owner reports written organizer approval for Irene; retain that correspondence privately and follow its exact scope. Eligibility requires 2–4 members, all 21+, working professionals, legally resident and physically based in JAPAC; full- and part-time students are excluded. One team and one submission per person. Only two finalists may attend, including self-funded attendees. Visa arrangements remain participants’ responsibility. The terms include a six-month right of first refusal over exclusive IP licensing/acquisition and broad promotional rights. Read these before submission; ownership should not be described as unrestricted. Terms take precedence over conflicting promotional text. [Official terms](https://docs.google.com/document/d/e/2PACX-1vRm7ChZ6Ij9fG7uFDkxzUMpwgVeBmnQ6cMDnAIEEX84AiLBOOQ9cYbl3S5OzFBbcVb8TF55s-eVpiXb/pub).

The supplied dashboard redirects to sign-in. Its private fields, additional rules, organizer correspondence and precise cutoff remain unverified. Public overview, requirements, FAQ, prizes and terms were reviewed.

## Product proposition

Irene helps adults find affordable therapist-led groups without making their private intake visible to peers. Start with one use case: students and early-career adults seeking support around stress, social anxiety or loneliness. Targeting students as users is separate from the competition’s restriction on student team members. A therapist defines the group purpose and decides suitability; AI helps organize the intake and prepare the review.

The innovation to demonstrate is the transition from an individual’s private account to a useful group-level brief: participants can correct their summary, therapists see why a group was proposed, and peers never receive each other’s intake. Group therapy already exists; validate differentiation through interviews rather than claiming invention or clinical superiority.

## Participant journey

1. Explain privacy boundaries, the role of AI, and who can see each field. Use an alias in group-facing surfaces. Explain confidentiality limits before consent.
2. Collect only useful information: adult age band, language, time zone, availability, goals, concerns, sharing preferences and budget. Start with text; avoid medical-document uploads.
3. Generate a constrained summary. The participant edits or confirms it before matching. Store the original, corrected version and consent version separately with restricted access.
4. Present a proposed circle: purpose, therapist, language, time, expected size, exact fee, and a plain-language reason it may fit. Never expose others’ stories or imply a compatibility percentage is a clinical assessment.
5. Offer accept, decline, request another group and request individual review. Do not imply that an invitation is a paid booking.
6. Provide a private preparation page and optional post-session feedback. Reminders should use discreet text and require opt-in.

## Therapist journey

Verify credentials and jurisdiction before granting a therapist role. Let the therapist define group goals, screening criteria, session length, capacity, schedule and fee. Review participants individually, approve or reject suggestions with reasons, then approve the group as a whole. Require renewed review when intake changes. A group proposal must not send invitations until the therapist explicitly approves it.

Matching must combine explicit constraints with semantic similarity: language, time-zone-aware schedule overlap, availability, fee limit, readiness and therapist-defined suitability. Similar concerns alone do not establish suitability. Group composition and preparation require clinical judgment; confidentiality among members cannot be guaranteed. [AGPA practice guidelines](https://agpa.org/guidelines-ethics/practice-guidelines-for-group-psychotherapy-2/).

The current risk indicator is an experimental review aid. Unknown or failed assessments must not become LOW. Potential urgent concerns should stop routine matching and display verified, region-appropriate urgent-help options. A stored review flag does not mean anyone has been notified. A real pilot needs a staffed process and a qualified clinician’s review of screening, crisis handling and informed consent.

## Meaningful Google services

| Service | Product purpose | Delivery decision |
| --- | --- | --- |
| Gemini on Vertex AI | Structured intake and therapist preparation drafts | Google adapter implemented; live verification pending |
| Google embeddings | Semantic comparison within a consistent model space | Adapter and provenance implemented; evaluate quality |
| Cloud Run | Java API and compiled React app, same origin | Container and private deployment script prepared |
| Cloud SQL PostgreSQL | Existing relational data and transactional group changes | Java connector added; instance not provisioned |
| Secret Manager | Database password and signing secret | Deployment references prepared; secrets not created |
| Cloud Build and Artifact Registry | Tested image builds and versioned releases | Build definition prepared |
| Cloud Logging and Monitoring | Availability, errors and request latency | Configure metrics and alerts; never log intake bodies |
| Sensitive Data Protection | Detect/redact identifiers before model calls | Implemented before Vertex generation; live verification pending |
| Firebase Authentication | Managed participant login and verified therapist claims | Later migration if it fits the time budget; do not maintain two parallel auth systems |
| Calendar and Meet | Therapist scheduling and session links | Manual therapist Meet link and private ICS export implemented; no Calendar API/OAuth |
| Cloud Tasks or Scheduler | Retryable, discreet opt-in reminders | Add after invitations and consent work |

Choose services for a demonstrated need. Cloud SQL preserves the existing relational design; adding Firestore alongside it would duplicate storage without a current benefit. A small Java workflow is sufficient; autonomous agents are unnecessary for clinical placement.

Google Sensitive Data Protection can mask identified information, but redaction is not a guarantee of anonymity. Configure detectors and evaluate indirect identifiers too. [Documentation](https://docs.cloud.google.com/sensitive-data-protection/docs/deidentify-sensitive-data).

Vertex AI does not train on customer data without permission, but retention depends on features and account terms. Review abuse logging, caching and region settings before a real pilot. Do not use Search grounding with private intake. [Data governance](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/vertex-ai-zero-data-retention).

Calendar can generate Meet conference details, but a calendar invite or signed-in video call may expose identities. Use fictional accounts in the demo, discreet titles, hidden attendee lists where supported, and verify the actual meeting experience. [Calendar documentation](https://developers.google.com/workspace/calendar/api/guides/create-events).

## Economics to validate

The landing-page illustration assumes a ₹2,400 total session fee: four participants pay ₹600 each, six pay ₹400, eight pay ₹300. These are arithmetic examples, not market prices, a quote or evidence of equivalent outcomes to individual therapy.

Before publishing fees, account for therapist preparation/screening time, session length, platform and payment costs, tax, subsidies, cancellations and unfilled seats. Never increase the fee after a participant accepts without renewed agreement. Consider a fixed participant price with a minimum enrollment threshold. Ask therapists what size and format they can responsibly facilitate; the cheapest group is not necessarily appropriate.

## Twelve days of delivery

| Day | Date | Deliverable and acceptance condition |
| --- | --- | --- |
| 1 | Oct 6 | Confirm organizer approval scope, preserve old work, remove OCI and credential defaults, close role escalation; establish tested Google adapter |
| 2 | Oct 7 | Create approved cloud project, enable APIs, deploy private synthetic prototype; prove real Gemini call and Cloud SQL persistence |
| 3 | Oct 8 | Interview one qualified therapist and 3–5 prospective users; agree screening workflow and smallest target segment; record consented, nonclinical feedback |
| 4 | Oct 9 | Intake wizard with participant summary correction, separate AI/participation consent and visible data-access explanation |
| 5 | Oct 10 | Privacy layer, deletion workflow and therapist assignment; prove one participant cannot read another’s intake |
| 6 | Oct 11 | Cohort proposals with constraints, explanations and budget/time-zone handling; finalize competition team roster |
| 7 | Oct 12 | Individual suitability review plus explicit group approval; persist invitation accept/decline, and prevent stale or duplicate placement |
| 8 | Oct 13 | Scheduling and private session preparation; Meet only if identity and host controls are verified |
| 9 | Oct 14 | Test 40–60 synthetic intake cases: matching, uncertain risk, prompt injection, missing fields, multilingual input, refusals and service outages |
| 10 | Oct 15 | Keyboard/mobile/accessibility testing, latency/cost measurements, failure recovery and secret scan; publish accurate demo limitations |
| 11 | Oct 16 | English PDF deck and 2:40–2:50 video; public source cleanup; independent teammate tests the deployed demo |
| 12 | Oct 17 | Freeze features; verify every submission link signed out, rehearse and submit; retain Oct 18 for corrections |

Suggested team split: product/frontend; backend/AI; infrastructure/testing; user research/demo. Combine roles for a two-person team. Clinical validation must involve an actual qualified reviewer, not a teammate pretending to be one.

## Evidence for judges

- Technical: live Google requests, service architecture, model/version provenance, measured latency and failure tests.
- Impact: interview findings, transparent fee arithmetic, screening/preparation time measured in a simulated workflow. Separate observations from hypotheses.
- Innovation: show corrected participant summaries, explicit matching constraints and a therapist’s override affecting the proposed group.
- UX: three roles with clear boundaries; mobile-friendly intake; understandable waiting and failure states; accessible controls.

Track extraction accuracy against manually labelled synthetic cases, inappropriate suggestions, privacy leakage, review overrides and successful end-to-end completion. Do not report synthetic results as clinical validation or invent user impact numbers.

## Three minute demo outline

0:00–0:20: problem and intended users. 0:20–0:55: fictional intake and editable AI summary. 0:55–1:35: therapist reviews matching reasons and an excluded/uncertain case. 1:35–2:05: group approval and participant acceptance with fee breakdown. 2:05–2:30: Google architecture and one real evaluation result. 2:30–2:50: limits, next pilot and impact hypothesis. Leave ten seconds below the strict limit.

## Scope exclusions for this deadline

No AI therapist, diagnosis, medication recommendations, custom video stack, session recording, emotion detection, automatic clinical placement, insurance billing or real payments. Defer voice input and additional languages until consent, screening and the primary journey work reliably. A polished narrow workflow is the release target.

## Current delivery status

Implemented: participant summary correction, professional verification, assigned therapist review queue, budget and schedule validation, group approval, private invitations and responses, session preparation/completion, feedback, export/deletion and Vertex DLP calls. There is no staffed clinical review service, real payment collection, automatic Meet creation or reminder delivery.

Twenty-five automated tests and a browser walkthrough exercise the local synthetic app. Live Google calls, DLP behavior, PostgreSQL migration/deployment and real matching quality still require an account and infrastructure. The finished local demonstration must not be described as a deployed or clinically validated service. Prioritize deployment, evaluation and user/therapist feedback during the remaining days instead of adding more features.
