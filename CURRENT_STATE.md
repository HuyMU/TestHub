# CURRENT_STATE.md — Implementation State & Project Memory

> [!IMPORTANT]
> This document is the Single Source of Truth for the **actual implementation status** of TestFlow Lite. Every developer and AI agent MUST update this file alongside feature commits whenever a module status changes.

- **Last Updated**: 2026-08-18
- **Checked Commit**: `22b65e7` (Branch: `main`)

---

## 1. Product Summary

TestFlow Lite (TestHub) is a lightweight, high-efficiency Test Case management and execution system designed for small QA teams (< 10 members: 1 Leader + up to 9 Testers). It provides streamlined Test Case workflows (`Draft` → `Review` → `Ready`), 2-step Excel Import/Export, Test Run & Milestone tracking, automated test result API ingestion, and project dashboards.

---

## 2. Module Implementation Status Matrix

| Module | Status | Summary & Verification |
|---|:---:|---|
| **Auth** | `Complete` | JWT Access + Refresh token flow, login via username/email, 401 automatic token refresh interceptor, `DisabledException` (403) handling. `JwtAuthFilter` checks `userDetails.isEnabled()`, returning 401 for deactivated accounts. |
| **Users** | `Complete` | Single Leader seed via `LeaderSeeder`, Leader CRUD & active status toggle for Testers, Leader update target protection (404), personal password change, user profile `/api/users/me`. |
| **Projects** | `Complete` | Leader CRUD for Projects (Name, Description, Status [Active/Archived]), role-aware project visibility (Leader sees all, Tester sees assigned), member assignment/removal for active Testers, Leader member assignment rejection (400). |
| **Sections** | `Complete` | Hierarchical Section & Subsection tree CRUD, drag-and-drop / batch reordering with UI client-side circular drop prevention, batch count queries resolving N+1 performance issue, circular reference checks, assigned Tester edit permissions, Leader-only delete guard returning 409 Conflict if section has child subsections or test cases. |
| **Test Cases** | `Complete` | Test Case CRUD with global `TC-%04d` code auto-generation, section binding, multi-field filtering & pagination, clone/duplicate support, owner Tester edit/delete permissions in `Draft` status, read-only mode for non-owners, automatic status reversion (`Ready` → `Draft`) upon Tester edit. Restricted `submitForReview` strictly to owning Testers (Leader attempts return 403 Forbidden). Added `submitted_at` timestamp tracking for accurate FIFO Review Queue ordering. |
| **Review Workflow** | `Complete` | 3-state lifecycle (`Draft` → `Review` → `Ready`). Tester submit for review (`Draft` → `Review`) with edit lock and `submitted_at` timestamping, Leader approve (`Review` → `Ready` with `reviewedBy`/`reviewedAt`) and reject (`Review` → `Draft` + required comment + `submitted_at` reset to null), global FIFO Review Queue workbench for Leaders sorted by `submitted_at ASC`. |
| **Excel Import/Export** | `Complete` | Apache POI 2-step Excel import engine (`/import/validate` returning line-by-line error reports & staging to `excel_import_sessions`; `/import/confirm` auto-creating missing section hierarchies & inserting `Draft` cases). Supports Full Section Path in Column A (`"Section Path"` header: `Parent > Child > Subchild`) independent of sheet tab names, with automatic fallback to root Section `"Uncategorized"` for empty path cells. Header `"Subsection Path"` (cell A0) automatically triggers per-sheet **LEGACY MODE** for 100% backward compatibility. Formatted `.xlsx` template generator (`/import/template`) and custom export engine (`/export`) with full path Column A output, Times New Roman 13pt styling, sheet-per-root-section layout, dynamic row height, and dynamic column hiding for Test Data / Automation Status. |
| **Milestones** | `Complete` | Leader-only Milestone CRUD (`/api/projects/{projectId}/milestones`), duplicate name protection, deletion guard returning 409 Conflict if referenced by existing Test Runs, frontend `MilestoneListPage.tsx` workbench. |
| **Test Runs** | `Complete` | Test Run creation selecting `READY` cases (with optional `includeNonReady` switch), milestone linking, Tester assignment per case, snapshotting content fields to `test_run_cases`, closing run (`POST /api/runs/{id}/close`), frontend `TestRunListPage.tsx`, `CreateTestRunModal.tsx`, `TestRunDetailPage.tsx`. |
| **Execution** | `Complete` | Manual execution recording (`PASSED`/`FAILED`/`BLOCKED`/`RETEST`) with mandatory `resultStatus` validation, assignment check & closed run guard (409), execution history append logging (`execution_history`), Leader result review (`Reviewed` approve or `Request Retest` with mandatory comment). Returns `latestExecutionHistoryId` for multi-file attachment linkage. Unified `verifyRunCaseAccess()` and `verifyExecutionHistoryAccess()` guards eliminate IDOR vulnerabilities across history and attachment endpoints (403 for non-project members). |
| **Automation API** | `Complete` | API Token management (`/api/tokens`) storing SHA-256 digests (`token_hash`, `length = 255`) in `api_tokens` table. Plaintext token returned ONCE upon generation (`ApiTokenCreatedDto`). Automated result ingestion (`POST /api/automation/results`) authenticated via `X-API-TOKEN` header (Rule 8 public HTTP layer). Ingestion resolves `TestRun` and `TestCase` by `code` (`TC-%04d`), updates `TestRunCase` result status, appends `ExecutionHistory` with `duration_ms` (V4 migration), and updates token `last_used_at`. Frontend `ApiTokenPage.tsx` wired with token creation modal and revocation Popconfirm. |
| **Attachments** | `Complete` | Real 1-to-many local filesystem upload (`POST /api/attachments/upload`) tied to `EXECUTION_HISTORY`, file format validation (`image/*`, `application/pdf`), 10MB size limit. Public `/uploads/**` path closed; secure authenticated endpoint `GET /api/attachments/{id}/file` enforces project member authorization (200/403/401). Internal `filePath` field removed from REST responses to sanitize disk file path exposure. |
| **Audit Logs** | `Complete` | `AuditLogService` writes audit records (`CREATE_TESTER`, `UPDATE_TESTER`, `CHANGE_PASSWORD`, `CREATE_PROJECT`, `UPDATE_PROJECT`, `ASSIGN_PROJECT_MEMBERS`, `REMOVE_PROJECT_MEMBER`, `CREATE_SECTION`, `UPDATE_SECTION`, `DELETE_SECTION`, `REORDER_SECTIONS`, `CREATE_TEST_CASE`, `UPDATE_TEST_CASE`, `DELETE_TEST_CASE`, `SUBMIT_TEST_CASE`, `APPROVE_TEST_CASE`, `REJECT_TEST_CASE`, `CLONE_TEST_CASE`, `IMPORT_VALIDATE_EXCEL`, `IMPORT_CONFIRM_EXCEL`, `EXPORT_EXCEL`, `CREATE_MILESTONE`, `UPDATE_MILESTONE`, `DELETE_MILESTONE`, `CREATE_TESTRUN`, `ADD_CASES_TO_RUN`, `CLOSE_TESTRUN`, `EXECUTE_TEST_CASE`, `REVIEW_TEST_RESULT`, `CREATE_API_TOKEN`, `REVOKE_API_TOKEN`, `AUTOMATION_SUBMIT_RESULT`) to `audit_logs` table. `AuditLogController` serves filtered audit trail (`GET /api/audit-logs`, Leader only). |
| **Dashboard & Reporting** | `Complete` | Real-time project metrics (`GET /api/dashboard/{projectId}`: total/ready/reviewQueue cases, passed/failed/blocked/retest/untested counts, milestone progress breakdown), Test Run JSON report & formatted Excel export (`GET /api/runs/{id}/report` & `GET /api/runs/{id}/report/export`), frontend `DashboardPage.tsx` and `TestRunDetailPage.tsx` report modal & export buttons. Tested via `DashboardControllerIntegrationTest` and `TestRunReportIntegrationTest`. |
| **Frontend** | `Complete` | Layout, Navigation, Auth (Login), User Management, Project Management, Section Management (`SectionTree`), Test Case Management (`TestCaseList`), Review Workflow (`ReviewQueuePage`), Excel Import/Export (`ImportWizardModal`, `ExportSectionPickerModal`), Milestone Management (`MilestoneListPage`), Test Run & Execution Management (`TestRunListPage`, `CreateTestRunModal`, `TestRunDetailPage`, `ExecuteResultModal`, `ReviewResultModal`), API Token Management (`ApiTokenPage.tsx`), and Dashboard & Reporting (`DashboardPage.tsx`) fully integrated. Clean 1-level response unwrap across all API modules. |

---

## 3. Incomplete & Stub Modules Detail

*All core modules (Slices 1 through 9) have been fully implemented and verified.*

---

## 4. Current Environment & Operational Notes

1. **Integration Testing**:
   - Integration tests (`AuthControllerIntegrationTest`, `UserControllerIntegrationTest`, `ProjectControllerIntegrationTest`, `SectionControllerIntegrationTest`, `TestCaseControllerIntegrationTest`, `ExcelControllerIntegrationTest`, `MilestoneControllerIntegrationTest`, `TestRunControllerIntegrationTest`, `ExecutionControllerIntegrationTest`, `ApiTokenControllerIntegrationTest`, `AutomationControllerIntegrationTest`) use **Testcontainers MySQL 8**. Docker daemon must be running locally for Testcontainers to spin up test databases.
2. **Database Migration & Seeding**:
   - Database migrations managed by Flyway (`V1__init_schema.sql`, `V2__add_architecture_decisions_schema.sql`, `V3__add_submitted_at_to_test_cases.sql`, `V4__add_duration_ms_to_execution_history.sql`).
   - Default Leader account is seeded automatically on application startup by `LeaderSeeder.java`.
3. **Open Decisions & Security Notes**:
   - *JWT Refresh Token Storage*: Currently stored in `localStorage` in frontend client (`authStore.ts`). XSS risk flagged; migration to `httpOnly` cookie deferred.
   - *Deactivated Account JWT Revocation*: `JwtAuthFilter` checks `userDetails.isEnabled()` on every request, denying authentication (401 Unauthorized) for deactivated users. Full JWT blocklist/redis cache deferred to Phase 2.
   - *React i18n Import in SectionTree*: `import { useTranslation } from 'react-i18next'` in `SectionTree.tsx` is intentionally retained for Phase 2 multi-language support (do NOT remove as dead code).
   - *Excel Export/Import Round-trip Sheet Name Mismatch*: **Resolved in Slice 10**. Full Section Path mode writes full section path into Column A (`"Section Path"`) during export. Re-importing evaluates Column A full path, completely eliminating dependency on truncated/sanitized sheet names (>31 chars) and avoiding duplicate section creation.

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

**Phase 2 Planning / Advanced QA Workflows**

- **Completed in Slice 10**:
  1. **Access Control Consolidation**: Centralized project membership and Leader-bypass checks across 9 backend services (`AttachmentService`, `MilestoneService`, `ProjectService`, `SectionService`, `ExcelService`, `DashboardService`, `TestRunService`, `TestCaseService`, `ExecutionService`) into a unified `ProjectAccessGuard` component.
  2. **Excel Import Session Project Match Verification**: In `ExcelService.confirmImport()`, added guard verifying `session.getProject().getId().equals(projectId)` to prevent cross-project session confirmation.
- **Recommended Next Priorities**:
  1. **HTTP-only Cookie Migration**: Migrate JWT Refresh Token storage from `localStorage` to secure HTTP-only cookies.
  2. **Testcontainers / Integration Suite Health**: Run full integration test suite when local Docker daemon is active.

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
9. **Excel Sheet-per-Section Layout (2026-08-08)**: Each sheet in import/export `.xlsx` represents one root Section. Column A `Subsection Path` (`"Parent > Child"`) places the case within the subtree.
10. **Auto-Create Section Hierarchy on Import (2026-08-08)**: Missing root sections or subsection paths are automatically created at Import Confirm time without erroring.
11. **Format-Agnostic Import Parsing (2026-08-08)**: Cell styling (font, color, bold) is ignored; only cell text content is parsed.
12. **Steps/Expected Result Numbered Correspondence (2026-08-08)**: Flexible step markers (`1.`, `1)`, `1:`, `Step 1:`, case-insensitive, extra whitespace tolerated). The `Expected Result` cell only contains entries for step numbers that produce an observable result — a step number is simply absent from `Expected Result` if that step has no distinct result to check.
13. **Excel Import Section Count & Sort Order Fix (2026-08-08)**: `resolveTargetSection()` returns newly created section count to accurately report `createdSectionsCount` in `confirmImport()`. Auto-created subsections assign `sortOrder` dynamically based on existing children under parent. Batch saving used for test case insertion.
14. **Milestone & Test Run Snapshotting (2026-08-08)**: Milestones are managed per-project (Leader-only). Test Runs select `READY` test cases (optional `includeNonReady` switch), link milestones, assign Testers per case, snapshot content fields into `test_run_cases` upon creation/addition, and support closing run (`POST /api/runs/{id}/close`).
15. **Add Cases to Run Ready Status Validation (2026-08-08)**: `AddCasesToRunRequest` includes `includeNonReady` boolean flag (default false) to enforce the FR-22 Ready status validation rule consistently when adding cases to an existing open Test Run.
16. **SRS Baseline Scoping Alignment (2026-08-08)**: SRS §7 and §9 reverted to v3.0 baseline per scope alignment; detailed live schema & REST API contracts are tracked in AI_CONTEXT.md §3.2 & §5 and docs/architecture/api-contracts.md.
17. **ResultStatus Enum Consolidation (2026-08-10)**: Standardized on `com.testhub.testflowlite.testrun.ResultStatus` (UPPERCASE: `PASSED`, `FAILED`, `BLOCKED`, `RETEST`, `UNTESTED`) across backend and frontend, eliminating duplicate PascalCase enum.
18. **Execution History 1-to-Many Attachment Linkage & Project-Level Authorization (2026-08-10)**: Executing a case (`POST /api/runs/{id}/cases/{caseId}/execute`) returns `latestExecutionHistoryId`. Attachments are uploaded afterwards via `POST /api/attachments/upload` with `entityType=EXECUTION_HISTORY` and `entityId={latestExecutionHistoryId}` supporting multiple files per execution attempt. Public `/uploads/**` static path is removed from `SecurityConfig`. Secure endpoint `GET /api/attachments/{id}/file` streams file resources after verifying project-level membership (returns 403 for non-members, 401 unauthenticated).
19. **Unified Execution Authorization & Attachment DTO Path Sanitization (2026-08-10)**: Standardized project membership authorization in `ExecutionService` via `verifyRunCaseAccess()` and `verifyExecutionHistoryAccess()`, eliminating IDOR vulnerabilities across history and attachment listing endpoints (returns 403 Forbidden for non-project members). Internal `filePath` field removed from `AttachmentDto` and REST API JSON responses to prevent disk file structure leakage.
20. **Review Submission Timestamp & Owning Tester submitForReview Guard (2026-08-10)**: `submitForReview()` restricted strictly to owning Testers (`Role.TESTER` and `createdBy == currentUser`). Leader calls return 403 Forbidden. Flyway migration `V3__add_submitted_at_to_test_cases.sql` adds `submitted_at` DATETIME column. `submitForReview()` sets `submitted_at = LocalDateTime.now()`, while `rejectTestCase()` clears `submitted_at = null` to give resubmissions fresh FIFO queue ordering (`findByStatusOrderBySubmittedAtAsc`). Dead code enums (`Priority`, `AutomationStatus`, `CaseStatus` in `common/`) and unreferenced `ImportModal.tsx` deleted.
21. **API Token Management & Automated Result Ingestion (2026-08-10)**: Implemented API token management (`com.testhub.testflowlite.apitoken`) storing SHA-256 digests (`token_hash`) in `api_tokens` table. Plaintext token (`thk_...`) returned ONCE upon generation (`ApiTokenCreatedDto`). Implemented automated result ingestion (`POST /api/automation/results`) authenticated via `X-API-TOKEN` header (public HTTP layer per Rule 8). Ingestion resolves `TestRun` and `TestCase` by `code` (`TC-%04d`), updates `TestRunCase` result status, appends `ExecutionHistory`, and updates token `last_used_at`. Rebuilt frontend `ApiTokenPage.tsx` with copy-once token modal and token revocation Popconfirm.
22. **Post-Slice-8 Code Review Bugfix (2026-08-10)**: Fixed `api_tokens.token_hash` column length mismatch (`length = 255`) matching V2 Flyway migration for Hibernate schema validation. Corrected duplicated `/api` prefix in `apiTokenApi.ts` (`/tokens`). Fixed systemic double `.data.data` response unwrap across all 7 feature API files (`testCaseApi.ts`, `sectionApi.ts`, `milestoneApi.ts`, `testRunApi.ts`, `excelApi.ts`, `executionApi.ts`, `apiTokenApi.ts`). Added Flyway migration `V4__add_duration_ms_to_execution_history.sql` (`duration_ms` column) and updated `ExecutionHistory`, `ExecutionHistoryDto`, and `AutomationService.submitResult()` to persist automation duration. Added `isEnabled()` check in `JwtAuthFilter.java` to deny requests from deactivated accounts (returning 401 Unauthorized).
23. **Dashboard Aggregation & Run Execution Reporting Architecture (2026-08-10)**: Implemented real-time dashboard metrics (`GET /api/dashboard/{projectId}`) aggregating repository total/ready/reviewQueue test cases, pass/fail/blocked/retest/untested counts across project runs, and milestone progress. Implemented Test Run JSON report (`GET /api/runs/{id}/report`) and formatted Excel report export (`GET /api/runs/{id}/report/export`) with Times New Roman 13pt styling and sheet summary blocks. Registered custom `JwtAuthenticationEntryPoint` returning HTTP 401 Unauthorized for unauthenticated requests. Integrated frontend `DashboardPage.tsx` and `TestRunDetailPage.tsx` report modal & export buttons. Tested via `DashboardControllerIntegrationTest` and `TestRunReportIntegrationTest`.
24. **Axios 1-Level Response Unwrap Frontend Bugfix (2026-08-10)**: Corrected `dashboardApi.getDashboard` return signature to `Promise<DashboardDto>` and updated `DashboardPage.tsx` to handle unwrapped payload directly, resolving false-positive error rendering ("Failed to load dashboard metrics"). Corrected `exportTestRunReport` in `testRunApi.ts` to use `new Blob([response.data || response])` with MIME type `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, preventing 9-byte corrupt `"undefined"` file creation on blob responseType downloads. Added explicit developer warning comment in `axiosClient.ts` to document 1-level response unwrap semantics.
25. **Allow Null Test Case Code Pending Generation (2026-08-11)**: Added Flyway migration `V6__allow_null_code_pending_generation.sql` modifying `test_cases.code` column to `VARCHAR(20) NULL` (preserving UNIQUE constraint). Resolves `Column 'code' cannot be null` database exception during initial `save()` / `saveAll()` prior to `TC-%04d` ID-based code generation across single creation, cloning, and Excel import.
26. **Excel Full Section Path Import & Backward Compatibility (2026-08-11)**: Redesigned Excel import section resolution to use full section paths in Column A (`Section Path` header, e.g. `Payment > Checkout > Validation`) independent of sheet tab names. Blank Column A cells map to default root Section `Uncategorized`. Format is detected per-sheet based on row 0 cell A0: header `Subsection Path` triggers per-sheet **LEGACY MODE** (sheet name = root section, Column A = relative path), while any other text (including `Section Path`) triggers **FULL_PATH MODE**. Export outputs full section path in Column A including root section name, guaranteeing 100% round-trip section alignment even for truncated sheet names (>31 chars). Disabled "Add Test Case" button in frontend `TestCaseList.tsx` with Tooltip when `sections.length === 0`. Verified via `ExcelServiceUnitTest` (5 unit tests pass) and `ExcelControllerIntegrationTest`.
27. **Access Control Consolidation & Import Session Project Guard (2026-08-18)**: Centralized project-level membership checks across 9 backend services (`MilestoneService`, `ProjectService`, `SectionService`, `ExcelService`, `TestCaseService`, `DashboardService`, `TestRunService`, `ExecutionService`, `AttachmentService`) into `ProjectAccessGuard` (`verifyProjectAccess` returning resolved `User` or throwing HTTP 403 `ForbiddenException`, and `hasProjectAccess(projectId, userId, role)` for boolean composition). Added strict project verification in `ExcelService.confirmImport()` (`session.getProject().getId().equals(projectId)`) to prevent cross-project import session confirmation. Direct queries to `projectMemberRepository.existsByProjectIdAndUserId` eliminated across all service call sites except `ProjectService.assignMembers` (pre-insert duplication check). Verified with 13 unit tests across `ProjectAccessGuardUnitTest` and `ExcelServiceUnitTest`.
28. **Project Existence Check in ProjectAccessGuard (2026-08-18)**: Restored centralized project existence verification inside `ProjectAccessGuard.verifyProjectAccess` (`if (!projectRepository.existsById(projectId)) throw new ResourceNotFoundException("Project not found: " + projectId)`). Resolves regression introduced during access control consolidation where `DashboardService.getDashboard`, `ExcelService.generateTemplate`, and `ExcelService.validateImport` lost existence validation when called by Leaders or Testers on non-existent project IDs. `hasProjectAccess` intentionally omits the check to maintain zero overhead on already-resolved entity paths. Verified with 22 unit tests across `ProjectAccessGuardUnitTest`, `ExcelServiceUnitTest`, and `DashboardServiceUnitTest`.
