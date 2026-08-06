# ADR 0001: Record Architecture Decisions — TestFlow Lite

- **Status**: Accepted
- **Date**: 2026-08-06
- **Context**: Setting up the project architecture and technology foundation for TestFlow Lite based on SRS v3.0.

## Decisions

### 1. Monolithic Package-by-Feature Backend Structure
- **Decision**: Package backend code by domain feature (`auth`, `user`, `project`, `section`, `testcase`, etc.) rather than technical layers (`controllers`, `services`, `repositories`).
- **Rationale**: Keeps related domain logic, DTOs, and operations localized in single feature packages, reducing cross-package navigation complexity for small teams.

### 2. Single Pre-Seeded Leader Account Strategy
- **Decision**: The system will support strictly 1 Leader account pre-seeded via `scripts/seed-leader.sql`. No UI interface will be created to register or add additional Leader accounts.
- **Rationale**: Fits SRS requirement for team size < 10 under a single lead, preventing unauthorized creation of administrative accounts.

### 3. API Token Authentication for Automation Result Ingestion
- **Decision**: Automation endpoints (`/api/automation/results`) use a separate `X-API-TOKEN` header authentication mechanism rather than user JWTs.
- **Rationale**: Allows CI/CD automation test runners to execute tests without maintaining user session JWTs or expiring user credentials.

### 4. 2-Step Excel Import Process
- **Decision**: Excel imports must run through `validate` (returns line errors without DB changes) followed by `confirm` (persists rows to DB in `Draft` state).
- **Rationale**: Guarantees data validity before database persistence and allows users to preview errors before committing large test sets.
