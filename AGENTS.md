# AGENTS.md — General Guidelines for AI Coding Agents

This document defines standard operational guidelines for any AI coding agent (Antigravity, Claude Code, GitHub Copilot Workspace, Codex, ChatGPT, etc.) working within the **TestFlow Lite (TestHub)** repository.

---

## 1. Mandatory Session Initialization Order

At the beginning of every session or task execution, you **MUST** read the following files in order before writing or modifying any code:

1. [CLAUDE.md](./CLAUDE.md) — Operational guidelines & core non-negotiable business rules.
2. [AI_CONTEXT.md](./AI_CONTEXT.md) — Domain reference, state machines, database schema summary, and API catalog.
3. [CURRENT_STATE.md](./CURRENT_STATE.md) — Source of Truth for verified implementation state and active task slice.
4. Run `git status` and `git log --oneline -10` to inspect recent repository activity.
5. Task-specific source files relevant to your current slice.

---

## 2. Core Operational Rules

### Scope & Requirement Scoping
- **Strict Scope Boundaries**: Implement strictly what is specified in the task prompt. Do NOT add unrequested features, future phase tasks, or extra architectural abstractions.
- **Single Source of Truth**: [DacTa-TestFlowLite-SRS.md](./DacTa-TestFlowLite-SRS.md) v3.0 strictly takes precedence over all other docs and assumptions.

### Architecture & Code Structure
- **Package-by-Feature (Backend)**: Keep controllers, services, repositories, and DTOs inside their domain package (e.g. `com.testhub.testflowlite.project.*`).
- **DTO Encapsulation**: NEVER expose JPA Entities directly across REST API endpoints. Always convert Entities to/from DTOs.
- **Unified API Response**: All REST controller responses MUST be wrapped in `ApiResponse<T>`.
- **Role & Permission Checks**: Enforce role authorizations strictly at the service and controller levels using `@PreAuthorize("hasRole('LEADER')")` or `isAuthenticated()`.
- **Frontend Organization**: Store feature components, hooks, and API calls under `src/features/<feature>/`. Keep reusable, non-domain components in `src/components/`.

### Git Safety & Version Control
- **Never Overwrite Unrelated Work**: Do NOT run `git reset --hard` or overwrite uncommitted work without explicit user consent.
- **Atomic Commits**: Keep commits focused and descriptive using Conventional Commit syntax (e.g. `feat(project): add member assignment API`).
- **Diff Verification**: Always check your `git diff` before finalizing a commit to ensure no unintended modifications or temp files are included.

### Verification & Testing
- **Never Claim Success Without Verification**: Always execute build and test commands to verify your changes.
- **Backend Verification**: Run `mvn clean test` (or `& "path/to/mvn.cmd" clean test`) and verify unit and Testcontainers integration tests pass.
- **Frontend Verification**: Run `npm run build` in `frontend/` to verify TypeScript types and Vite bundle creation without errors.
- **Report Executed Verification**: State clearly what verification commands were run and report any environment limitations.

### Documentation Maintenance
- **Update Implementation State**: When a module or feature changes status, update the matrix in [CURRENT_STATE.md](./CURRENT_STATE.md).
- **Update API Catalog**: When endpoints, payloads, or schemas change, update [AI_CONTEXT.md](./AI_CONTEXT.md).

---

## 3. Standard Handoff Format

When completing a task turn or preparing to hand off work to another agent or session, summarize your work using the following structure:

```markdown
### Handoff Summary
- **Done**: Brief summary of completed slice/feature.
- **Changed Files**: List of modified or created files.
- **Verification**: Commands executed and test results (e.g., `mvn clean test` pass, `npm run build` pass).
- **Known Limits / Blockers**: Any environment constraints or deferred logic.
- **Next Task**: The exact next recommended task slice.
```
