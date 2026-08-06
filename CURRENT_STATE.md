# CURRENT_STATE.md — Implementation State & Project Memory

> [!IMPORTANT]
> This document is the Single Source of Truth for the **actual implementation status** of TestFlow Lite. Every developer and AI agent MUST update this file alongside feature commits whenever a module status changes.

- **Last Updated**: 2026-08-06
- **Checked Commit**: `e594957` (Branch: `main`)

---

## 1. Product Summary

TestFlow Lite (TestHub) is a lightweight, high-efficiency Test Case management and execution system designed for small QA teams (< 10 members: 1 Leader + up to 9 Testers). It provides streamlined Test Case workflows (`Draft` → `Review` → `Ready`), 2-step Excel Import/Export, Test Run & Milestone tracking, automated test result API ingestion, and project dashboards.

---

## 2. Module Implementation Status Matrix

| Module | Status | Summary & Verification |
|---|:---:|---|
| **Auth** | `Complete` | JWT Access + Refresh token flow, login via username/email, 401 automatic token refresh interceptor, `DisabledException` (403) handling. |
| **Users** | `Complete` | Single Leader seed via `LeaderSeeder`, Leader CRUD & active status toggle for Testers, Leader update target protection (404), personal password change, user profile `/api/users/me`. |
| **Projects** | `Complete` | Leader CRUD for Projects (Name, Description, Status [Active/Archived]), role-aware project visibility (Leader sees all, Tester sees assigned), member assignment/removal for active Testers, Leader member assignment rejection (400). |
| **Sections** | `Complete` | Hierarchical Section & Subsection tree CRUD, drag-and-drop / batch reordering, circular reference checks, assigned Tester edit permissions, Leader-only delete guard returning 409 Conflict if section has child subsections or test cases. |
| **Test Cases** | `Stub` | Skeleton DTOs & `TestCaseController` returning empty case lists. Logic pending Slice 4. |
| **Review Workflow** | `Stub` | Skeleton endpoints for `submit-review`, `approve`, `reject`, and `review-queue`. Logic pending Slice 4. |
| **Excel Import/Export** | `Stub` | Skeleton endpoints for `/import/validate`, `/import/confirm`, and `/export`. Staging table schema `excel_import_sessions` added in V2 migration. Logic pending Slice 5. |
| **Milestones** | `Stub` | Skeleton DTOs & `MilestoneController` returning empty lists. Logic pending Slice 6. |
| **Test Runs** | `Stub` | Skeleton DTOs & `TestRunController` returning empty lists. Snapshot schema added in V2 migration. Logic pending Slice 6. |
| **Execution** | `Stub` | Skeleton endpoints for `/execute` and `/review`. Logic pending Slice 7. |
| **Automation API** | `Stub` | Skeleton endpoint `POST /api/automation/results` with `X-API-TOKEN`. `token_hash` & `revoked_at` schema added in V2 migration. Logic pending Slice 8. |
| **Attachments** | `Stub` | Skeleton endpoint for local filesystem upload to `/uploads`. Logic pending Slice 8. |
| **Audit Logs** | `Complete` | `AuditLogService` writes audit records (`CREATE_TESTER`, `UPDATE_TESTER`, `CHANGE_PASSWORD`, `CREATE_PROJECT`, `UPDATE_PROJECT`, `ASSIGN_PROJECT_MEMBERS`, `REMOVE_PROJECT_MEMBER`, `CREATE_SECTION`, `UPDATE_SECTION`, `DELETE_SECTION`, `REORDER_SECTIONS`) to `audit_logs` table. `AuditLogController` serves filtered audit trail (`GET /api/audit-logs`, Leader only). |
| **Dashboard** | `Stub` | Skeleton `DashboardController` returning mock 0 metrics. Logic pending Slice 9. |
| **Frontend** | `Partial` | Layout, Navigation, Auth (Login), User Management (List Testers, Create/Edit Tester modal, Change Password modal), Project Management (List Projects, Create/Edit Project modal, Project Detail workbench with Members tab), Section Management (Interactive `SectionTree` with tree view, create/edit modal, 409 error modal, Leader delete button) fully integrated. Placeholder pages exist for Test Cases, Runs, Milestones, API Tokens. |

---

## 3. Incomplete & Stub Modules Detail

- **Test Cases & Review Workflow (`testcase/`)**:
  - *Current Code*: Skeleton `TestCaseController.java`, `TestCaseService.java`, `TestCaseDto.java`.
  - *Missing*: `TestCase` entity, 3-state transition checks (`Draft` → `Review` → `Ready`), Tester vs Leader editing rules, review queue queries.
- **Excel Import/Export (`excel/`)**:
  - *Current Code*: Skeleton `ExcelController.java`, `ExcelService.java`, `ExcelImportResultDto.java`.
  - *Missing*: Apache POI parser, 2-step validation engine, line error reporting, staging table `excel_import_sessions` integration.
- **Milestones & Test Runs (`milestone/`, `testrun/`)**:
  - *Current Code*: Skeleton controllers and services returning empty lists.
  - *Missing*: `Milestone` and `TestRun` JPA entities, case selection filters (only `Ready` cases), snapshot columns in `test_run_cases` ingestion logic.
- **Execution & Automation Result Ingestion (`execution/`, `automation/`)**:
  - *Current Code*: Skeleton controllers.
  - *Missing*: `TestRunCase` execution state recording, execution history logging, Leader result review, API Token SHA-256 hash verification (`token_hash`, `revoked_at`).
- **Dashboard (`dashboard/`)**:
  - *Current Code*: Skeleton `DashboardController.java` returning static zero metrics.
  - *Missing*: Database aggregation queries for Pass/Fail/Blocked rates, Review Queue counter, Milestone progress.

---

## 4. Current Environment & Operational Notes

1. **Integration Testing**:
   - Integration tests (`AuthControllerIntegrationTest`, `UserControllerIntegrationTest`, `ProjectControllerIntegrationTest`, `SectionControllerIntegrationTest`) use **Testcontainers MySQL 8**. Docker daemon must be running locally for Testcontainers to spin up test databases.
2. **Database Migration & Seeding**:
   - Database migrations managed by Flyway (`V1__init_schema.sql`, `V2__add_architecture_decisions_schema.sql`).
   - Default Leader account is seeded automatically on application startup by `LeaderSeeder.java`.
3. **Open Security Notes**:
   - *JWT Refresh Token Storage*: Currently stored in `localStorage` in frontend client (`authStore.ts`). XSS risk flagged; migration to `httpOnly` cookie deferred.

---

## 5. Verified Run & Build Commands

### Backend Verification
```bash
# Compile and run all unit + Testcontainers integration tests
cd backend && mvn clean test

# Run Spring Boot backend locally
cd backend && mvn spring-boot:run
```

### Frontend Verification
```bash
# Type check and build production bundle
cd frontend && npm run build

# Start Vite development server
cd frontend && npm run dev
```

### Containerized Environment
```bash
# Copy template and launch database + backend services
cp .env.example .env
docker-compose up -d --build
```

---

## 6. Recommended Next Task

**Slice 4: Test Case Management & Review Workflow**
- *Scope*: Implement FR-11 through FR-18 in package `testcase/` and frontend `features/testcases/`.
- *Details*: `TestCase` JPA entity with code auto-generation (e.g. `TC-0001`), section binding, and 3-state lifecycle (`Draft` → `Review` → `Ready`). Tester submit for review (`Draft` → `Review`), Leader approve (`Review` → `Ready`) and reject (`Review` → `Draft` + comment). Tester editing a `Ready` case automatically reverts it to `Draft`; Leader editing a `Ready` case keeps it in `Ready`. Review queue endpoint `GET /api/cases/review-queue`.

---

## 7. Memory Maintenance Protocol for Agents

Whenever an AI agent completes a task that alters feature implementations or status:
1. Re-verify backend compilation with `mvn clean test` and frontend build with `npm run build`.
2. Update the status matrix in `CURRENT_STATE.md` with the new commit hash and date.
3. Update [AI_CONTEXT.md](./AI_CONTEXT.md) catalog if APIs or data models were modified.

---

## 8. Architecture Decisions Log

1. **Section Deletion Guard (2026-08-06)**: Deletion of a Section is blocked with HTTP `409 Conflict` if it contains child Subsections or Test Cases. Cascade delete is forbidden.
2. **Section Edit Permissions (2026-08-06)**: Any Tester assigned to a project can create or edit any Section within that project. Section deletion remains Leader-only.
3. **Test Run Case Snapshotting (2026-08-06)**: Test case content fields (`title`, `precondition`, `steps`, `expected_result`, `test_data`) are copied into `test_run_cases` when added to a run to ensure point-in-time immutability for execution and reporting.
4. **API Token Hash Storage (2026-08-06)**: `api_tokens.token` stored as SHA-256 hash (`token_hash`). Plaintext shown once upon generation. Tokens support revocation via `revoked_at`.
5. **Excel Import Staging Session (2026-08-06)**: Staging table `excel_import_sessions` stores parsed preview payloads between `/import/validate` and `/import/confirm` via `importSessionId` with expiration (`expires_at`).
