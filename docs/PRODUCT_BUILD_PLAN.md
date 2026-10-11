# Irene product and delivery status

Updated 12 October 2026. This document supersedes the initial twelve-day build plan and records the delivered prototype. Deployment, public source, the prescribed-template deck and narrated video are complete. The organizer-dashboard submission is a separate team action.

## Why Irene exists

The problem is making therapy or therapist-led support more affordable, with a private first step for people who find it hard to ask for help. Irene began with the experience of needing therapy in college, finding it expensive, and hesitating to tell parents because they would worry.

Our solution is therapist-led group support with a shared session fee. A person can describe their concerns privately before deciding whether to join a circle. The intended societal contribution is a lower financial barrier, control over disclosure, and access to professionally guided connection. These are impact goals, not measured clinical outcomes.

## Delivered participant and professional journeys

1. A participant registers, gives consent, and records concerns, goals, language, availability and budget.
2. Gemini prepares a structured summary and review signals. The participant can correct and confirm the summary; peers cannot read the intake.
3. The assigned therapist reviews suitability. High or uncertain risk is excluded from routine matching.
4. Matching combines practical constraints, related concerns, goals and Google embeddings. The therapist selects participants and saves a proposal.
5. Final approval rechecks current intake, schedule, availability and budget before issuing private invitations with fixed per-person fees.
6. Participants accept, decline or withdraw. Approved circles provide preparation and calendar downloads; therapists can supply a Meet URL, cancel or complete sessions. Private feedback, data export and intake deletion are implemented.
7. Professional applications require administrator verification before therapist access. The three sign-in entry points do not grant privileges by themselves.

## Services used in the delivered release

| Service | Actual use |
| --- | --- |
| Firebase Hosting | Live React/Vite frontend, HTTPS and SPA routing |
| Cloud Run | Live Java/Spring Boot API in Mumbai, minimum 0 and maximum 2 instances |
| Gemini Developer API | Structured intake summaries, experimental review signals and private facilitator drafts; deployed generation model `gemini-3.5-flash-lite` |
| Google embeddings | `gemini-embedding-001`, 768-dimensional vectors supporting constraint-based matching |
| Supabase PostgreSQL | Deployed relational database, private schema, Flyway migrations and certificate-verified TLS |
| Secret Manager | Runtime credentials and database CA certificate, accessed by a restricted service identity |
| Cloud Build and Artifact Registry | Verified backend container build and versioned image storage |
| Google Stitch | Design-time studies informing the implemented coral-and-ivory interface |
| Google Fonts | Playfair Display and Plus Jakarta Sans typography with system fallbacks |
| Google Meet / calendar files | Therapist-supplied Meet URL and private downloadable ICS; no automatic room creation or Calendar API integration |
| Gemini speech generation | Aoede narration for the final 2:41 demonstration video; a submission asset, not an in-app voice feature |

Firestore, Firebase Authentication, Gemma and ADK are not integrated. Irene uses its own role-based authentication. Vertex AI, Sensitive Data Protection and Cloud SQL have alternative code/configuration paths but are not part of this deployment or prerequisites for the completed demo. The current hosted Gemini path uses basic email masking, not comprehensive de-identification. Automated reminders and payment collection are outside the delivered scope.

## Delivered evidence

- [Live application](https://project-irene-fd39e.web.app)
- [Public source](https://github.com/TheAverageGatsby13/project-irene)
- [16-slide prescribed-template PDF](submission/Irene-AI-Builder-Cup.pdf) and [editable PowerPoint](submission/Irene-AI-Builder-Cup.pptx)
- [2:41 narrated video](https://drive.google.com/file/d/1VxKqMv-1Tg9g7m6cUbuaK8eWOmsG7w3e/view), viewable by anyone with the link; both deck formats include it
- [Submission description](submission/Brief-Description.txt), within the form's 1,024-character limit
- [Release verification](RELEASE_VERIFICATION.md): 30 backend tests, 6 frontend routing tests, production build and 35 live workflow checks recorded for the published release
- [Deployment inventory and rollback](DEPLOYMENT_STATUS.md)

Team: **Model Citizens**. Team leader: **Debadrita Dutta**. Theme: **Sustainability & Social Impact**. The team reports written organizer approval for the rebuild and retains that correspondence privately. Confirm the organizer dashboard's exact category and submission fields when submitting.

## Economics and next-stage validation

The cost illustration divides a ₹2,400 total fee across four people at ₹600 each, six at ₹400 or eight at ₹300. These are arithmetic examples, not evidence of market pricing or equivalent outcomes to individual therapy. The implemented invitation fixes the quoted participant fee; there is no payment transaction.

A real service would need qualified clinical review, user and therapist research, screening and crisis-response operations, evaluation of group suitability, abuse controls, recovery testing and sustainable pricing. No real care or meeting was delivered by the recorded checks. These are requirements for a later real-user pilot, not unfinished features of the fictional-data demonstration. The app does not diagnose, replace a therapist or monitor emergencies.
