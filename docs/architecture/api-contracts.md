# API Endpoint Contracts Specification — TestFlow Lite

> [!NOTE]
> This document details the exact REST API endpoint specifications implemented across Spring Boot REST Controllers.
> Synchronized with `AI_CONTEXT.md` §5.

---

## 1. Auth Module (`/api/auth`)

| Method | Endpoint | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| POST | `/api/auth/login` | `LoginRequest` (username/email, password) | `ApiResponse<TokenResponse>` (accessToken, refreshToken, user) | Public | User authentication |
| POST | `/api/auth/refresh` | `RefreshTokenRequest` (refreshToken) | `ApiResponse<TokenResponse>` | Public | Issue new access token |

---

## 2. User Management Module (`/api/users`)

| Method | Endpoint | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| GET | `/api/users/me` | - | `ApiResponse<UserDto>` | Authenticated | Get current authenticated user profile |
| PUT | `/api/users/me/password` | `ChangePasswordRequest` | `ApiResponse<Void>` | Authenticated | Change current user password |
| GET | `/api/users` | - | `ApiResponse<List<UserDto>>` | Leader | List all Tester accounts |
| POST | `/api/users` | `CreateUserRequest` | `ApiResponse<UserDto>` | Leader | Create a new Tester account |
| PUT | `/api/users/{id}` | `UpdateUserRequest` | `ApiResponse<UserDto>` | Leader | Update Tester account / active status |

---

## 3. Project Management Module (`/api/projects`)

| Method | Endpoint | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| GET | `/api/projects` | - | `ApiResponse<List<ProjectDto>>` | Authenticated | List visible projects |
| POST | `/api/projects` | `CreateProjectRequest` | `ApiResponse<ProjectDto>` | Leader | Create project |
| PUT | `/api/projects/{id}` | `UpdateProjectRequest` | `ApiResponse<ProjectDto>` | Leader | Update project details/status |
| POST | `/api/projects/{id}/members` | `AssignMembersRequest` | `ApiResponse<Void>` | Leader | Assign Testers to Project |
| DELETE | `/api/projects/{id}/members/{userId}` | - | `ApiResponse<Void>` | Leader | Remove Tester from Project |

---

## 4. Section & Subsection Module (`/api/projects/{projectId}/sections` & `/api/sections`)

| Method | Endpoint | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| GET | `/api/projects/{projectId}/sections` | - | `ApiResponse<List<SectionDto>>` | Authenticated | Get section hierarchy tree |
| POST | `/api/projects/{projectId}/sections` | `CreateSectionRequest` | `ApiResponse<SectionDto>` | Leader / Tester | Create Section or Subsection |
| PUT | `/api/sections/{id}` | `UpdateSectionRequest` | `ApiResponse<SectionDto>` | Leader / Tester | Rename section |
| DELETE | `/api/sections/{id}` | - | `ApiResponse<Void>` | Leader | Delete section (Blocked 409 if has child subsections or cases) |
| PUT | `/api/projects/{projectId}/sections/reorder` | `ReorderSectionsRequest` | `ApiResponse<Void>` | Leader / Tester | Reorder section tree |

---

## 5. Test Case Module (`/api/cases`)

| Method | Endpoint | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| GET | `/api/cases` | Filter parameters (`projectId`, `sectionId`, `keyword`, `priority`, `type`, `status`, `page`, `size`) | `ApiResponse<Page<TestCaseDto>>` | Authenticated | List/search Test Cases |
| POST | `/api/cases` | `CreateTestCaseRequest` | `ApiResponse<TestCaseDto>` | Leader / Tester | Create Test Case (`Draft`) |
| GET | `/api/cases/{id}` | - | `ApiResponse<TestCaseDto>` | Authenticated | Get Test Case details |
| PUT | `/api/cases/{id}` | `UpdateTestCaseRequest` | `ApiResponse<TestCaseDto>` | Leader / Tester | Edit Test Case (Tester edits `Ready` -> `Draft`) |
| DELETE | `/api/cases/{id}` | - | `ApiResponse<Void>` | Leader / Tester (Owner) | Delete Test Case |
| POST | `/api/cases/{id}/submit-review` | - | `ApiResponse<TestCaseDto>` | Tester (Owner) | Submit case for Leader review (`Draft` -> `Review`) |
| POST | `/api/cases/{id}/approve` | - | `ApiResponse<TestCaseDto>` | Leader | Approve case (`Review` -> `Ready`) |
| POST | `/api/cases/{id}/reject` | `RejectCommentRequest` | `ApiResponse<TestCaseDto>` | Leader | Reject case (`Review` -> `Draft` + comment) |
| POST | `/api/cases/{id}/clone` | - | `ApiResponse<TestCaseDto>` | Leader / Tester | Clone Test Case |
| GET | `/api/cases/review-queue` | Filter parameters | `ApiResponse<Page<TestCaseDto>>` | Leader | Global FIFO queue of cases pending review |

---

## 6. Excel Import / Export Module (`/api/projects/{projectId}/cases`)

| Method | Endpoint | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| POST | `/api/projects/{projectId}/cases/import/validate` | `MultipartFile` (`file`) | `ApiResponse<ExcelImportValidateResponse>` | Leader / Tester | Step 1: Validate rows & return error list + `importSessionId` |
| POST | `/api/projects/{projectId}/cases/import/confirm` | `ExcelImportConfirmRequest` | `ApiResponse<ExcelImportConfirmResponse>` | Leader / Tester | Step 2: Confirm import to DB (`Draft` state, auto-creates missing sections) |
| GET | `/api/projects/{projectId}/cases/import/template` | - | `.xlsx` file stream | Leader / Tester | Download formatted blank Excel import template |
| GET | `/api/projects/{projectId}/cases/export` | `sectionIds` (optional) | `.xlsx` file stream | Leader / Tester | Export Test Cases to Excel (Sheet-per-root-section layout) |

---

## 7. Milestone Module (`/api/projects/{projectId}/milestones`)

| Method | Endpoint | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| GET | `/api/projects/{projectId}/milestones` | - | `ApiResponse<List<MilestoneDto>>` | Authenticated | List Project Milestones |
| POST | `/api/projects/{projectId}/milestones` | `CreateMilestoneRequest` | `ApiResponse<MilestoneDto>` | Leader | Create Milestone (Name, Due Date) |
| PUT | `/api/projects/{projectId}/milestones/{id}` | `UpdateMilestoneRequest` | `ApiResponse<MilestoneDto>` | Leader | Update Milestone details / status |
| DELETE | `/api/projects/{projectId}/milestones/{id}` | - | `ApiResponse<Void>` | Leader | Delete Milestone (Blocked 409 if referenced by Test Runs) |

---

## 8. Test Run Module (`/api/projects/{projectId}/runs` & `/api/runs`)

| Method | Endpoint | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| GET | `/api/projects/{projectId}/runs` | - | `ApiResponse<List<TestRunDto>>` | Authenticated | List Test Runs for project |
| POST | `/api/projects/{projectId}/runs` | `CreateTestRunRequest` | `ApiResponse<TestRunDto>` | Leader | Create Test Run (Select `Ready` cases, link Milestone, assign Testers, content snapshot) |
| GET | `/api/runs/{id}` | - | `ApiResponse<TestRunDto>` | Authenticated | Get Test Run details with snapshotted cases |
| POST | `/api/runs/{id}/cases` | `AddCasesToRunRequest` | `ApiResponse<TestRunDto>` | Leader | Append cases to open run with content snapshotting |
| DELETE | `/api/runs/{id}/cases/{runCaseId}` | - | `ApiResponse<Void>` | Leader | Remove case from open run |
| POST | `/api/runs/{id}/close` | - | `ApiResponse<TestRunDto>` | Leader | Close Test Run (`status` = `Closed`, `closed_at` = `now()`) |

---

## 9. Automation Result API (`/api/automation`)

| Method | Endpoint | Header | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| POST | `/api/automation/results` | `X-API-TOKEN` | `AutomationResultDto` | `ApiResponse<Void>` | API Token | Direct automated test result ingestion |

---

## 10. Audit Log & Dashboard Modules (`/api/audit-logs` & `/api/dashboard`)

| Method | Endpoint | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| GET | `/api/audit-logs` | Filter parameters | `ApiResponse<Page<AuditLogDto>>` | Leader | Audit trail logs |
| GET | `/api/dashboard/{projectId}` | - | `ApiResponse<DashboardSummaryDto>` | Authenticated | Pass/Fail/Blocked rates, Review Queue count, Milestone progress |
