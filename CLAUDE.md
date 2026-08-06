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
| Import Excel (Test Cases) | ✅ | ✅ |
| Export Excel (Test Cases / Runs) | ✅ | ✅ |
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
│   ├── automation/         # Automation REST API (/api/automation/results with API token)
│   ├── attachment/         # Local filesystem upload handlers
│   ├── audit/              # Audit logging hooks & query endpoints
│   └── dashboard/          # Aggregated project statistics & completion metrics
```

---

## 6. Coding Conventions & Architecture Rules

### Backend Conventions
1. **Package-by-Feature**: Keep controllers, services, repositories, and DTOs within their feature package (e.g., `com.testhub.testflowlite.testcase.*`). Do NOT create a monolithic shared layer.
2. **DTO Encapsulation**: Never expose JPA Entities directly across REST endpoints. Map Entities to dedicated DTOs for requests and responses.
3. **Unified API Response**: Wrap all REST controller responses in `ApiResponse<T>` with standard status, code, message, and payload fields.
4. **Centralized Exception Handling**: Throw business exceptions (e.g., `ResourceNotFoundException`, `UnauthorizedException`) and handle them globally using `@RestControllerAdvice` in `GlobalExceptionHandler`.
5. **Database Auditing**: Extends `BaseEntity` (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`) for entities requiring tracking.

### Frontend Conventions
1. **Feature-based Structure**: Store feature components, API calls, and local state under `src/features/<feature_name>/`.
2. **Shared UI Components**: Put reusable, non-domain components (e.g., `StatusTag`, `ConfirmModal`, `PageHeader`) in `src/components/`.
3. **Centralized Axios Client**: Use `src/api/axiosClient.ts` with request/response interceptors for attaching JWT headers and automatic refresh token processing.
4. **State Management**: Use Zustand (`src/store/`) for global application state (authentication, current project context).

### Git & Commit Conventions
- Branch naming: `feature/<feature-name>`, `bugfix/<issue-name>`, `chore/<task-name>`
- Commit messages (Conventional Commits):
  - `feat(testcase): add submit for review endpoint`
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
