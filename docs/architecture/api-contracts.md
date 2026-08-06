# API Endpoint Contracts Specification — TestFlow Lite

## 1. Auth Module (`/api/auth`)

| Method | Endpoint | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| POST | `/api/auth/login` | `LoginRequest` (username/email, password) | `ApiResponse<TokenResponse>` (accessToken, refreshToken, user) | Public | User authentication |
| POST | `/api/auth/refresh` | `RefreshTokenRequest` (refreshToken) | `ApiResponse<TokenResponse>` | Public | Issue new access token |

---

## 2. User Management Module (`/api/users`)

| Method | Endpoint | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| GET | `/api/users` | - | `ApiResponse<List<UserDto>>` | Leader | List all Tester accounts |
| POST | `/api/users` | `CreateUserDto` | `ApiResponse<UserDto>` | Leader | Create a new Tester account |
| PUT | `/api/users/{id}` | `UpdateUserDto` | `ApiResponse<UserDto>` | Leader | Update Tester account / active status |
| POST | `/api/users/change-password` | `ChangePasswordDto` | `ApiResponse<Void>` | Leader / Tester | Change current user password |

---

## 3. Project Management Module (`/api/projects`)

| Method | Endpoint | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| GET | `/api/projects` | - | `ApiResponse<List<ProjectDto>>` | Authenticated | List visible projects |
| POST | `/api/projects` | `CreateProjectDto` | `ApiResponse<ProjectDto>` | Leader | Create project |
| PUT | `/api/projects/{id}` | `UpdateProjectDto` | `ApiResponse<ProjectDto>` | Leader | Update project details/status |
| POST | `/api/projects/{id}/members` | `AssignMembersDto` | `ApiResponse<Void>` | Leader | Assign Testers to Project |

---

## 4. Section & Subsection Module (`/api/projects/{projectId}/sections`)

| Method | Endpoint | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| GET | `/api/projects/{id}/sections` | - | `ApiResponse<List<SectionTreeDto>>` | Authenticated | Get section hierarchy tree |
| POST | `/api/projects/{id}/sections` | `CreateSectionDto` | `ApiResponse<SectionDto>` | Leader / Tester | Create Section or Subsection |
| PUT | `/api/sections/{id}` | `UpdateSectionDto` | `ApiResponse<SectionDto>` | Leader / Tester | Rename or reorder section |
| DELETE | `/api/sections/{id}` | - | `ApiResponse<Void>` | Leader | Delete section |

---

## 5. Test Case Module (`/api/cases`)

| Method | Endpoint | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| GET | `/api/cases` | Filter parameters | `ApiResponse<Page<TestCaseDto>>` | Authenticated | List/search Test Cases |
| POST | `/api/cases` | `CreateTestCaseDto` | `ApiResponse<TestCaseDto>` | Leader / Tester | Create Test Case (`Draft`) |
| PUT | `/api/cases/{id}` | `UpdateTestCaseDto` | `ApiResponse<TestCaseDto>` | Leader / Tester | Edit Test Case (Tester edits `Ready` -> `Draft`) |
| POST | `/api/cases/{id}/submit-review` | - | `ApiResponse<TestCaseDto>` | Tester | Submit case for Leader review (`Draft` -> `Review`) |
| POST | `/api/cases/{id}/approve` | - | `ApiResponse<TestCaseDto>` | Leader | Approve case (`Review` -> `Ready`) |
| POST | `/api/cases/{id}/reject` | `RejectCommentDto` | `ApiResponse<TestCaseDto>` | Leader | Reject case (`Review` -> `Draft` + comment) |
| GET | `/api/cases/review-queue` | Filter parameters | `ApiResponse<List<TestCaseDto>>` | Leader | Queue of cases pending review |

---

## 6. Excel Import / Export Module (`/api/cases/import` & `/api/cases/export`)

| Method | Endpoint | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| POST | `/api/cases/import/validate` | `MultipartFile` (Excel) | `ApiResponse<ExcelValidationResultDto>` | Leader / Tester | Step 1: Validate rows & return error list |
| POST | `/api/cases/import/confirm` | `ImportTokenDto` | `ApiResponse<ImportSummaryDto>` | Leader / Tester | Step 2: Confirm import to DB (`Draft` state) |
| GET | `/api/cases/export` | Filter parameters | Excel file (`byte[]`) | Leader / Tester | Export Test Cases to Excel |

---

## 7. Milestone Module (`/api/milestones`)

| Method | Endpoint | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| GET | `/api/projects/{projectId}/milestones` | - | `ApiResponse<List<MilestoneDto>>` | Authenticated | List Project Milestones |
| POST | `/api/milestones` | `CreateMilestoneDto` | `ApiResponse<MilestoneDto>` | Leader | Create Milestone (Name, Due Date) |
| PUT | `/api/milestones/{id}` | `UpdateMilestoneDto` | `ApiResponse<MilestoneDto>` | Leader | Update Milestone details / status |

---

## 8. Test Run & Execution Module (`/api/runs`)

| Method | Endpoint | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| GET | `/api/projects/{projectId}/runs` | - | `ApiResponse<List<TestRunDto>>` | Authenticated | List Test Runs for project |
| POST | `/api/runs` | `CreateTestRunDto` | `ApiResponse<TestRunDto>` | Leader | Create Test Run (Select `Ready` cases, link Milestone) |
| POST | `/api/runs/{id}/cases/{caseId}/execute` | `ExecutionResultDto` | `ApiResponse<Void>` | Leader / Tester | Record execution result |
| POST | `/api/runs/{id}/cases/{caseId}/review` | `ResultReviewDto` | `ApiResponse<Void>` | Leader | Review execution result (Reviewed / Retest) |
| GET | `/api/runs/{id}/report` | - | `ApiResponse<RunReportDto>` | Authenticated | Fetch full Run execution report |

---

## 9. Automation Result API (`/api/automation`)

| Method | Endpoint | Header | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| POST | `/api/automation/results` | `X-API-TOKEN` | `AutomationResultDto` | `ApiResponse<Void>` | API Token | Direct automated test result ingestion |

---

## 10. Dashboard Module (`/api/dashboard`)

| Method | Endpoint | Request Body | Response Payload | Auth | Description |
|---|---|---|---|---|---|
| GET | `/api/dashboard/{projectId}` | - | `ApiResponse<DashboardSummaryDto>` | Authenticated | Pass/Fail/Blocked rates, Review Queue count, Milestone progress |
