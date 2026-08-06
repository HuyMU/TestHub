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
| **Sections** | `Stub` | Skeleton DTOs & `SectionController` returning empty section tree. Logic pending Slice 3. |
| **Test Cases** | `Stub` | Skeleton DTOs & `TestCaseController` returning empty case lists. Logic pending Slice 4. |
| **Review Workflow** | `Stub` | Skeleton endpoints for `submit-review`, `approve`, `reject`, and `review-queue`. Logic pending Slice 4. |
| **Excel Import/Export** | `Stub` | Skeleton endpoints for `/import/validate`, `/import/confirm`, and `/export`. Logic pending Slice 5. |
| **Milestones** | `Stub` | Skeleton DTOs & `MilestoneController` returning empty lists. Logic pending Slice 6. |
| **Test Runs** | `Stub` | Skeleton DTOs & `TestRunController` returning empty lists. Logic pending Slice 6. |
| **Execution** | `Stub` | Skeleton endpoints for `/execute` and `/review`. Logic pending Slice 7. |
| **Automation API** | `Stub` | Skeleton endpoint `POST /api/automation/results` with `X-API-TOKEN`. Logic pending Slice 8. |
| **Attachments** | `Stub` | Skeleton endpoint for local filesystem upload to `/uploads`. Logic pending Slice 8. |
| **Audit Logs** | `Partial` | `AuditLogService` writes audit records (`CREATE_TESTER`, `UPDATE_TESTER`, `CHANGE_PASSWORD`, `CREATE_PROJECT`, `UPDATE_PROJECT`, `ASSIGN_PROJECT_MEMBERS`, `REMOVE_PROJECT_MEMBER`) to `audit_logs` table, but `AuditLogController` query endpoint remains stub. |
| **Dashboard** | `Stub` | Skeleton `DashboardController` returning mock 0 metrics. Logic pending Slice 9. |
| **Frontend** | `Partial` | Layout, Navigation, Auth (Login), User Management (List Testers, Create/Edit Tester modal, Change Password modal), Project Management (List Projects, Create/Edit Project modal, Project Detail workbench with Members tab assignment/removal UI) fully integrated. Placeholder pages exist for Sections, Test Cases, Runs, Milestones, API Tokens. |

---

## 3. Incomplete & Stub Modules Detail

- **Sections & Subsections (`section/`)**:
  - *Current Code*: Skeleton `SectionController.java`, `SectionService.java`, `SectionDto.java`.
  - *Missing*: Self-referencing tree entity `Section`, parent section validations, reordering, deletion constraints.
- **Test Cases & Review Workflow (`testcase/`)**:
  - *Current Code*: Skeleton `TestCaseController.java`, `TestCaseService.java`, `TestCaseDto.java`.
  - *Missing*: `TestCase` entity, 3-state transition checks (`Draft` → `Review` → `Ready`), Tester vs Leader editing rules, review queue queries.
- **Excel Import/Export (`excel/`)**:
  - *Current Code*: Skeleton `ExcelController.java`, `ExcelService.java`, `ExcelImportResultDto.java`.
  - *Missing*: Apache POI parser, 2-step validation engine, line error reporting, Excel workbook generator.
- **Milestones & Test Runs (`milestone/`, `testrun/`)**:
  - *Current Code*: Skeleton controllers and services returning empty lists.
  - *Missing*: `Milestone` and `TestRun` JPA entities, case selection filters (only `Ready` cases), status closure logic.
- **Execution & Automation Result Ingestion (`execution/`, `automation/`)**:
  - *Current Code*: Skeleton controllers.
  - *Missing*: `TestRunCase` execution state recording, execution history logging, Leader result review, API Token verification.
- **Dashboard (`dashboard/`)**:
  - *Current Code*: Skeleton `DashboardController.java` returning static zero metrics.
  - *Missing*: Database aggregation queries for Pass/Fail/Blocked rates, Review Queue counter, Milestone progress.

---

## 4. Current Environment & Operational Notes

1. **Maven Execution on Local Windows Environment**:
   - Maven binary is located at `C:\Users\Nhat Huy\.vscode\extensions\oracle.oracle-java-25.1.0\nbcode\java\maven\bin\mvn.cmd` or can be executed via containerized Docker environment.
2. **Integration Testing**:
   - Integration tests (`AuthControllerIntegrationTest`, `UserControllerIntegrationTest`, `ProjectControllerIntegrationTest`) use **Testcontainers MySQL 8**. Docker daemon must be running locally for Testcontainers to spin up test databases.
3. **Database Migration & Seeding**:
   - Database migrations managed by Flyway (`V1__init_schema.sql`).
   - Default Leader account is seeded automatically on application startup by `LeaderSeeder.java`.

---

## 5. Verified Run & Build Commands

### Backend Verification
```bash
# Compile and run all unit + Testcontainers integration tests
& "C:\Users\Nhat Huy\.vscode\extensions\oracle.oracle-java-25.1.0\nbcode\java\maven\bin\mvn.cmd" clean test

# Run Spring Boot backend locally
& "C:\Users\Nhat Huy\.vscode\extensions\oracle.oracle-java-25.1.0\nbcode\java\maven\bin\mvn.cmd" spring-boot:run
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

**Slice 3: Section & Subsection Management**
- *Scope*: Implement FR-09 & FR-10 in package `section/` and frontend `features/sections/`.
- *Details*: `Section` JPA entity with self-referencing `parent_section_id` supporting infinite depth hierarchy. Tree retrieval API (`GET /api/projects/{id}/sections`), creation/rename API (`POST`/`PUT`), and deletion API (`DELETE` - Leader only). Update Project Detail page Tab 1 ("Sections / Cases") to display the interactive Section tree.

---

## 7. Memory Maintenance Protocol for Agents

Whenever an AI agent completes a task that alters feature implementations or status:
1. Re-verify backend compilation with `mvn clean test` and frontend build with `npm run build`.
2. Update the status matrix in `CURRENT_STATE.md` with the new commit hash and date.
3. Update [AI_CONTEXT.md](./AI_CONTEXT.md) catalog if APIs or data models were modified.
