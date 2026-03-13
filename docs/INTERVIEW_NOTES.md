# Project Irene Interview Walkthrough Notes

## 2-minute motivation story (STAR)

- Situation: In college, private therapy was costly and hard to access discreetly.
- Task: Build a safer, affordable pathway where one therapist session can support multiple people with similar needs.
- Action: Designed Project Irene with AI-assisted intake normalization, risk screening, and therapist-reviewed matching.
- Result: Created an interview-ready full-stack prototype that demonstrates intake, matching, and cohort creation with clear ownership and safety boundaries.

## 8-minute architecture and code walkthrough

1. `backend/src/main/java/com/irene/irene/auth`
- JWT auth, role-based access (`PATIENT`, `THERAPIST`, `ADMIN`).
- Explain why auth is first: all sensitive endpoints depend on identity and role.

2. `backend/src/main/java/com/irene/irene/intake`
- Intake captures profile + consent + free-text concerns.
- `IntakeService` orchestrates persistence and AI pipeline invocation.

3. `backend/src/main/java/com/irene/irene/ai`
- Interface-first `AiService` design.
- `MockAiService` for deterministic local testing.
- `OciAiService` for real provider integration and retry/fallback behavior.

4. `backend/src/main/java/com/irene/irene/matching`
- Hard filters before scoring.
- Weighted score formula with explainability string.
- Persisted `match_candidates` run history.

5. `backend/src/main/java/com/irene/irene/groups`
- Therapist approves and creates group from selected candidates.

6. Frontend
- Login -> intake -> template -> run matching -> create group.
- Highlight responsive UI and deliberate visual style choices for demo impact.

## 8-minute AI usage and judgment decisions

- Where AI is used:
  - risk triage
  - issue/goal extraction
  - embeddings for similarity support
- Where AI is not used:
  - no autonomous clinical decision
  - no automatic final group assignment
- Human override point:
  - therapist explicitly approves final cohort
- Safety treatment:
  - high-risk intakes are excluded from regular matching flow

## 5-minute tradeoffs and risks

- Tradeoff: Modular monolith instead of microservices for speed and clarity.
- Tradeoff: CSV/text vector storage for MVP speed instead of dedicated vector DB.
- Risk: OCI endpoint/auth setup can block live AI calls.
- Mitigation: `mock` mode kept functional with same interface contract.
- Risk: Mental-health context is sensitive.
- Mitigation: synthetic data only, clear non-emergency acknowledgement, explicit consent.

## 5-minute Q&A backup answers

- Why Java-only now and no C++?
  - Interview sprint optimization; C++ reserved for future perf-critical scoring service.

- Why OCI over free Gemini key?
  - Better privacy/compliance posture and clearer enterprise story.

- How do you prove ownership if AI helped?
  - Show specific manual changes: scoring weights, role boundaries, error handling, and test design.

- What would you build next?
  - Attendance outcomes loop, therapist feedback signals, scheduling integration, and stronger compliance controls.
