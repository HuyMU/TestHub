# CURRENT_STATE.md — Implementation State & Project Memory

> [!IMPORTANT]
> This document is the Single Source of Truth for the **actual implementation status** of TestFlow Lite. Every developer and AI agent MUST update this file alongside feature commits whenever a module status changes.

- **Last Updated**: 2026-08-08
- **Checked Commit**: `c33d1d3` (Branch: `main`)

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
| **Sections** | `Complete` | Hierarchical Section & Subsection tree CRUD, drag-and-drop / batch reordering with UI client-side circular drop prevention, batch count queries resolving N+1 performance issue, circular reference checks, assigned Tester edit permissions, Leader-only delete guard returning 409 Conflict if section has child subsections or test cases. |
| **Test Cases** | `Complete` | Test Case CRUD with global `TC-%04d` code auto-generation, section binding, multi-field filtering & pagination, clone/duplicate support, owner Tester edit/delete permissions in `Draft` status, read-only mode for non-owners, and automatic status reversion (`Ready` → `Draft`) upon Tester edit. |
| **Review Workflow** | `Complete` | 3-state lifecycle (`Draft` → `Review` → `Ready`). Tester submit for review (`Draft` → `Review`) with edit lock, Leader approve (`Review` → `Ready` with `reviewedBy`/`reviewedAt`) and reject (`Review` → `Draft` + required comment), global FIFO Review Queue workbench for Leaders. |
| **Excel Import/Export** | `Stub` | Skeleton endpoints for `/import/validate`, `/import/confirm`, and `/export`. Staging table schema `excel_import_sessions` added in V2 migration. Logic pending Slice 5. |
| **Milestones** | `Stub` | Skeleton DTOs & `MilestoneController` returning empty lists. Logic pending Slice 6. |
| **Test Runs** | `Stub` | Skeleton DTOs & `TestRunController` returning empty lists. Snapshot schema added in V2 migration. Logic pending Slice 6. |
| **Execution** | `Stub` | Skeleton endpoints for `/execute` and `/review`. Logic pending Slice 7. |
| **Automation API** | `Stub` | Skeleton endpoint `POST /api/automation/results` with `X-API-TOKEN`. `token_hash` & `revoked_at` schema added in V2 migration. Logic pending Slice 8. |
| **Attachments** | `Stub` | Skeleton endpoint for local filesystem upload to `/uploads`. Logic pending Slice 8. |
| **Audit Logs** | `Complete` | `AuditLogService` writes audit records (`CREATE_TESTER`, `UPDATE_TESTER`, `CHANGE_PASSWORD`, `CREATE_PROJECT`, `UPDATE_PROJECT`, `ASSIGN_PROJECT_MEMBERS`, `REMOVE_PROJECT_MEMBER`, `CREATE_SECTION`, `UPDATE_SECTION`, `DELETE_SECTION`, `REORDER_SECTIONS`, `CREATE_TEST_CASE`, `UPDATE_TEST_CASE`, `DELETE_TEST_CASE`, `SUBMIT_TEST_CASE`, `APPROVE_TEST_CASE`, `REJECT_TEST_CASE`, `CLONE_TEST_CASE`) to `audit_logs` table. `AuditLogController` serves filtered audit trail (`GET /api/audit-logs`, Leader only). |
| **Dashboard** | `Stub` | Skeleton `DashboardController` returning mock 0 metrics. Logic pending Slice 9. |
| **Frontend** | `Partial` | Layout, Navigation, Auth (Login), User Management (List Testers, Create/Edit Tester modal, Change Password modal), Project Management (List Projects, Create/Edit Project modal, Project Detail workbench with Members tab), Section Management (`SectionTree`), Test Case Management (`TestCaseList` table, `TestCaseFormModal` 10-field form with read-only/lock mode, Submit/Clone/Delete actions), Review Workflow (`ReviewQueuePage` Leader workbench with Approve/Reject comment modal) fully integrated. Placeholder pages exist for Runs, Milestones, API Tokens. |

---

## 3. Incomplete & Stub Modules Detail

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
   - Integration tests (`AuthControllerIntegrationTest`, `UserControllerIntegrationTest`, `ProjectControllerIntegrationTest`, `SectionControllerIntegrationTest`, `TestCaseControllerIntegrationTest`) use **Testcontainers MySQL 8**. Docker daemon must be running locally for Testcontainers to spin up test databases.
2. **Database Migration & Seeding**:
   - Database migrations managed by Flyway (`V1__init_schema.sql`, `V2__add_architecture_decisions_schema.sql`).
   - Default Leader account is seeded automatically on application startup by `LeaderSeeder.java`.
3. **Open Decisions & Security Notes**:
   - *JWT Refresh Token Storage*: Currently stored in `localStorage` in frontend client (`authStore.ts`). XSS risk flagged; migration to `httpOnly` cookie deferred.
   - *React i18n Import in SectionTree*: `import { useTranslation } from 'react-i18next'` in `SectionTree.tsx` is intentionally retained for Phase 2 multi-language support (do NOT remove as dead code).

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

**Slice 5: Excel Import & Export (2-Step Validation & Generation)**
- *Scope*: Implement FR-17 through FR-20 in package `excel/` and frontend `features/excel/`.
- *Details*: Apache POI Excel import validation engine (`POST /api/cases/import/validate`) parsing uploaded template, returning row-by-row error list and persisting valid rows to `excel_import_sessions` staging table; confirmation endpoint (`POST /api/cases/import/confirm`) committing staging payload to DB in `Draft` status. Excel export endpoints (`GET /api/cases/export`, `GET /api/runs/{id}/export`).

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
6. **Test Case Ownership Rule (2026-08-08)**: Test Cases are owned by their creator (`created_by`). Only owner Tester or Leader may edit/delete in editable state (`Draft`). Other Testers have read-only access.
7. **Test Case Edit Lock & Reversion (2026-08-08)**: Test Case editing/deletion is locked for owner Tester once submitted to `Review`. Editing a `Ready` case as a Tester automatically reverts it to `Draft`. Editing as Leader preserves `Ready` status.
8. **Test Case Global Code Generation (2026-08-08)**: `code` (e.g. `TC-0001`) is generated globally from `TC-%04d` using auto-increment primary key `id` after insert.
