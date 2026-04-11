# AGENTS.md

## Project
Project Irene is an AI-assisted privacy-preserving matching engine for affordable therapist-led group sessions.

## Stack
- Java 17
- Spring Boot 3
- Maven
- PostgreSQL
- Flyway
- React + Vite
- JUnit 5

## Architecture Rules
- Keep backend as a modular monolith using feature-based packages.
- Keep controllers thin and move business logic into services.
- Add Flyway migrations for schema changes.
- Use DTOs at API boundaries; do not expose entities directly.
- All REST endpoints must include OpenAPI annotations.

## Security and Privacy Rules
- Use synthetic data only for demo and tests.
- Do not store or process real PHI.
- Add explicit consent flags and not-for-emergency acknowledgement fields.
- Maintain role-based access control for patient/therapist/admin.

## Testing Rules
- Add tests for services and critical controller flows.
- Include positive path and edge-case checks for matching and risk exclusion.

## Done Criteria
- Project compiles.
- Tests pass.
- README is updated with setup, architecture, and demo flow.
