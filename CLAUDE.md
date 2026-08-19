# CLAUDE.md — AI & Developer Operating Guidelines for TestFlow Lite

## 1. Project Summary

**TestFlow Lite (TestHub)** is a streamlined Test Case management and execution system designed for small QA teams (< 10 members: 1 Leader + up to 9 Testers). It provides a simple review workflow for Test Cases (`Draft` → `Review` → `Ready`), 2-step Excel Import/Export (Validate/Preview → Confirm), Test Run & Milestone execution tracking, automated test result ingestion via dedicated API Tokens, and real-time project dashboards.

---

## 2. Single Source of Truth

> [!IMPORTANT]
> Before implementing any feature or modification, you **MUST** read [DacTa-TestFlowLite-SRS.md](./DacTa-TestFlowLite-SRS.md).
> If this guidelines file (`CLAUDE.md`) or any other document conflicts with `DacTa-TestFlowLite-SRS.md`, **the SRS document strictly takes precedence**.

---

## 3. Roles & Permissions Matrix

| Feature / Action | Leader | Tester |
|---|:---:|:---:|
| Manage Tester Accounts | ✅ | ❌ |
| Create / Archive Projects | ✅ | ❌ |
| Assign Testers to Projects | ✅ | ❌ |
| CRUD Sections / Subsections | ✅ | ⚠️ Create/Edit only (No Delete) |
| Create / Edit Test Cases | ✅ | ✅ (Cases created by self, in Draft status) |
| Submit Test Case for Review | — | ✅ |
| Approve/Reject Test Case (`Review` → `Ready` / `Draft`) | ✅ | ❌ |
| Create Test Runs & Milestones | ✅ | ❌ |
| Execute Test Runs | ✅ | ✅ (Assigned cases) |
| Review Test Results | ✅ | ❌ |
| View Dashboard & Reports | ✅ (All projects) | ✅ (Assigned projects only) |
| Manage Automation API Tokens | ✅ | ❌ |

---

## 4. Tech Stack Specification

| Component | Technology |
|---|---|
| Backend Framework | Java 17+, Spring Boot 3.x, Spring Security, Spring Data JPA |
| Database | MySQL 8 |
| Authentication | JWT (Access Token + Refresh Token) |
| Frontend Framework | React 18+ (Vite), Ant Design 5.x, Axios, React Router v6 |
| Excel Processing | Apache POI |
| API Documentation | `springdoc-openapi` (Swagger UI at `/swagger-ui.html`) |
| UI Language | English (Default MVP). i18n structure prepared for Phase 2 |
| File Storage | Local filesystem (`/uploads`) for MVP attachments |
| Containerization | Docker Compose (App + MySQL) for MVP |
| Real-time Updates | Manual refresh in MVP (WebSocket/SSE deferred to Phase 2) |
| Automation Auth | Dedicated API Token header (independent of user JWT) |

---

## 5. Repository Architecture (Package-by-Feature)

```
TestHub/
├── backend/src/main/java/com/testhub/testflowlite/
│   ├── TestFlowLiteApplication.java
│   ├── config/             # Spring Security, JWT, OpenAPI, CORS configuration
│   ├── common/             # Base entity, global exception handling, response wrapper, enums
│   ├── security/           # JWT providers, filters, user details service
│   ├── auth/               # Login & refresh token endpoints
│   ├── user/               # Tester account management (Leader only)
│   ├── project/            # Project CRUD & project member assignments
│   ├── section/            # Tree hierarchy of Sections & Subsections
│   ├── testcase/           # Test Case CRUD, workflow (Draft/Review/Ready), clone, review queue
│   ├── excel/              # Apache POI import/export validation & confirmation
│   ├── milestone/          # Milestone management (name, due date)
│   ├── testrun/            # Test Run creation & case assignments
│   ├── execution/          # Execution state recording & Leader review
│   ├── apitoken/           # API token management & SHA-256 hash storage
│   ├── automation/         # Automation REST API (/api/automation/results with API token)
│   ├── attachment/         # Local filesystem upload handlers
│   ├── audit/              # Audit logging hooks & query endpoints
│   └── dashboard/          # Aggregated project statistics & completion metrics
```

---

## 6. Coding Conventions & Architecture Rules

### Backend Conventions
1. **Package-by-Feature**: Keep controllers, services, repositories, and DTOs within their feature package (e.g., `com.testhub.testflowlite.excel.*`). Do NOT create a monolithic shared layer.
2. **DTO Encapsulation**: Never expose JPA Entities directly across REST endpoints. Map Entities to dedicated DTOs for requests and responses.
3. **Unified API Response**: Wrap all REST controller responses in `ApiResponse<T>` with standard status, code, message, and payload fields.
4. **Centralized Exception Handling**: Throw business exceptions (e.g., `ResourceNotFoundException`, `UnauthorizedException`, `ConflictException`) and handle them globally using `@RestControllerAdvice` in `GlobalExceptionHandler`.
5. **Database Auditing**: Extends `BaseEntity` (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`) for entities requiring tracking.

### Frontend Conventions
1. **Feature-based Structure**: Store feature components, API calls, and local state under `src/features/<feature_name>/`.
2. **Shared UI Components**: Put reusable, non-domain components (e.g., `StatusTag`, `ConfirmModal`, `PageHeader`) in `src/components/`.
3. **Centralized Axios Client**: Use `src/api/axiosClient.ts` with request/response interceptors for attaching JWT headers and automatic refresh token processing.
4. **State Management**: Use Zustand (`src/store/`) for global application state (authentication, current project context).

### Git & Commit Conventions
- Branch naming: `feature/<feature-name>`, `bugfix/<issue-name>`, `chore/<task-name>`
- Commit messages (Conventional Commits):
  - `feat(excel): implement excel import validate and confirm endpoints`
  - `fix(auth): fix token expiration handling`
  - `docs(api): update swagger annotations for execution endpoint`

---

## 7. Non-Negotiable Business Rules

> [!CAUTION]
> AI agents and developers MUST NOT alter or add features outside these core SRS rules:

1. **Single Leader Rule**: There is **ONLY 1 Leader account** in the system, seeded via `LeaderSeeder.java`. NO UI shall exist for creating additional Leader accounts.
2. **Test Case Status Lifecycle**: Strictly 3 states: `Draft` → `Review` → `Ready`. Do NOT add `Rejected` or `Deprecated` states.
3. **Excel Import State**: All Test Cases imported from Excel **MUST start in `Draft` status**, regardless of whether imported by Leader or Tester.
4. **Ready Case Modification Rule**:
   - If a **Tester** edits a case in `Ready` status, it **MUST automatically revert to `Draft`**.
   - If a **Leader** edits a case in `Ready` status, it **remains in `Ready` status**.
5. **No Test Plan**: Test Plan entity has been completely removed in SRS v3.0. Do NOT introduce Test Plan tables or concepts.
6. **No Real-Time Server Push**: Real-time updates (WebSockets / SSE) are explicitly excluded from Phase 1 MVP. Use client manual refresh.
7. **Single Language MVP**: UI language is strictly **English** for Phase 1 MVP. Do NOT generate `vi.json` or add UI language switch toggles in MVP.
8. **Automation API Authentication**: Automation ingestion endpoint (`POST /api/automation/results`) MUST authenticate using an **API Token**, NOT user JWT tokens.
9. **Section Deletion Rule**: A Section CANNOT be deleted if it has child Subsections OR contains Test Cases. API MUST return `409 Conflict` with a clear message. Cascade delete is explicitly forbidden.
10. **Section Edit Permission**: Any Tester assigned to the project MAY create/edit ANY Section in that project (not limited to sections they created). Section deletion remains Leader-only (already defined in Roles Matrix).
11. **Test Run Snapshot Rule**: When a Test Case is added to a Test Run, its content fields (title, precondition, steps, expected_result, test_data) MUST be copied (snapshotted) into `test_run_cases` at that moment. Test execution and reporting MUST always read from the snapshot, NEVER live-join back to the current `test_cases` row.
12. **API Token Security**: API Tokens (`api_tokens.token`) MUST be stored as a SHA-256 hash (`token_hash`), never plaintext. The plaintext token is shown to the user ONLY ONCE at creation time. Tokens MUST support revocation via `revoked_at`.
13. **Excel Import Session Rule**: Between `/import/validate` and `/import/confirm`, parsed data MUST be persisted server-side in a staging table (`excel_import_sessions`) referenced by `importSessionId`, with an expiry (`expires_at`). Do NOT round-trip full parsed payloads through the client.
14. **Test Case Ownership Rule**: A Test Case is owned by the Tester who created it (`created_by`). Only the owner Tester OR the Leader may edit or delete it while it is editable (see Rule 15). Other Testers have READ-ONLY access (can view but not edit/delete).
15. **Test Case Edit Lock Rule**: A Test Case can only be edited/deleted by its owner Tester while status is `Draft`. Once submitted to `Review`, the owner Tester CANNOT withdraw or edit it — it is locked until the Leader approves (`Ready`) or rejects (`Draft` + comment). The Leader MAY edit a Test Case at any status, at any time, without ownership restriction.
16. **Test Case Code Generation**: `code` (e.g. `TC-0001`) is generated GLOBALLY (unique across the entire system, not per-project), derived deterministically from the entity's own auto-increment primary key after insert (format `TC-%04d`) to avoid race conditions — do NOT use a separate counter/sequence table.
17. **Excel Import / Export Section Path Layout**: In the import template and export file (.xlsx), Column A header is **"Section Path"** and contains the full hierarchical path from the project root (format `"Parent > Child > Grandchild"`). Sheet tab names are purely for human organization and do NOT define or infer the section hierarchy in Full Path mode. An empty Column A cell maps the test case to root section **"Uncategorized"** (auto-created if missing). For backward compatibility, if cell A0 contains the header **"Subsection Path"** (case-insensitive), the sheet is processed in **LEGACY MODE** (sheet name = root section, Column A = relative subsection path). Export outputs full paths in Column A under the header "Section Path" while organizing sheets per root section.
18. **Auto-Create Section Hierarchy on Import**: If a sheet name or Subsection Path segment does not match an existing Section/Subsection in the project, the system automatically creates it (reusing Section creation logic) at Import Confirm time — never blocks import with an error for this reason.
19. **Format-Agnostic Import Parsing**: Import parsing reads only cell TEXT content — font, size, color, bold/italic, alignment, or any other cell styling in the uploaded file is completely ignored and never affects recognition or validation.
20. **Steps/Expected Result Numbered Correspondence**: Within a single cell, each line is expected to start with a flexible numeric marker (`1.`, `1)`, `1:`, `Step 1:`, case-insensitive, extra whitespace tolerated). The `Expected Result` cell only contains entries for step numbers that produce an observable result — a step number is simply absent from `Expected Result` if that step has no distinct result to check.
21. **Review Submission Timestamp**: `test_cases.submitted_at` is set when a Tester submits a case for review (`Draft` → `Review`) and is the sort key for the review queue (`submitted_at ASC`), NOT `created_at`. When a Leader rejects a case (`Review` → `Draft`), `submitted_at` is cleared to `null` so that subsequent re-submission receives a fresh FIFO queue position.
22. **Add Test Case UI Guard**: A Test Case cannot be created via the UI when the project has zero Sections defined; the "Add Test Case" action in `TestCaseList.tsx` MUST be disabled with an explanatory tooltip (`"Create a Section first before adding Test Cases"`) in that state. Import and Export actions remain enabled.
23. **Centralized Project Access Control (`ProjectAccessGuard`)**: All services verifying project-level access for users MUST use `ProjectAccessGuard` (`verifyProjectAccess` or `hasProjectAccess`), which enforces that Leaders have universal access while Testers must have explicit membership in `project_members`. Direct duplicate queries via `projectMemberRepository.existsByProjectIdAndUserId(...)` outside of `ProjectAccessGuard` and `ProjectService.assignMembers` are forbidden.
24. **JWT Secret Deployment Configuration**: `JWT_SECRET` must be set via an environment variable for any real deployment; `application-prod.yml` enforces no fallback default and fails fast at startup if unset, while `JwtTokenProvider` emits a prominent log warning if the resolved secret matches the repository default.
25. **Sanitized Generic Error Responses**: `GlobalExceptionHandler.handleGenericException` must never interpolate `ex.getMessage()` or any exception-derived strings into client HTTP responses — only the fixed generic message (`"An unexpected error occurred. Please try again or contact support."`) is returned, while internal error details and stack traces belong strictly in server logs via `log.error`.
26. **HttpOnly Refresh Token Cookie & Strict CORS Origin**: The JWT refresh token is issued and transmitted strictly via an `HttpOnly`, `SameSite=Lax`, path-scoped (`/api/auth`) cookie (`refresh_token`) and is never included in JSON response bodies or stored in client-side storage (`localStorage`/`sessionStorage`). Access tokens are kept in-memory only. `CorsConfig` enforces an exact origin match against `app.frontend-origin` (no wildcards) when `allowCredentials(true)` is enabled.

---

## 8. Development & Testing Commands

### Backend Commands
```bash
# Build project and run unit/integration tests
cd backend && mvn clean test

# Run Spring Boot backend locally
cd backend && mvn spring-boot:run

# Package executable JAR
cd backend && mvn clean package -DskipTests
```

### Frontend Commands
```bash
# Install dependencies
cd frontend && npm install

# Start Vite dev server
cd frontend && npm run dev

# Type check & build production bundle
cd frontend && npm run build
```

---

## 9. Definition of Done (DoD) for Tasks

A task is considered complete ONLY when:
1. Backend compiles cleanly with `mvn clean test` (all unit and integration tests pass).
2. Frontend compiles cleanly with `npm run build` with zero TypeScript errors.
3. Code strictly complies with the **Non-Negotiable Business Rules** in Section 7.
4. OpenAPI / Swagger annotations are added/updated for modified backend endpoints.
5. All file paths in documentation use clickable Markdown links.
