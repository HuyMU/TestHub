# Task Handoff

> [!NOTE]
> Documented task handoff between AI agents, developer sessions, and project memory.

---

## 1. Goal
Implement **Slice 10: Access Control Consolidation + Excel Import Session Guard**:
1. Centralize the duplicated `projectMemberRepository.existsByProjectIdAndUserId(...)` membership checks across 9 backend services (14 call sites) into a single shared `ProjectAccessGuard` component.
2. Fix the IDOR gap in `ExcelService.confirmImport()` by validating that the staged `ExcelImportSession` project ID strictly matches the path `projectId`.

## 2. Current Branch & Status
- **Branch**: `main`
- **Status**: Complete & Verified (`mvn test-compile`, unit tests `13/13` pass, `npm run build` pass).

## 3. Work Completed
- **`ProjectAccessGuard` (`com.testhub.testflowlite.project`)**:
  - `verifyProjectAccess(Long projectId, String currentUsername)`: Resolves user via `userRepository` and returns `User` or throws HTTP 403 `ForbiddenException("You do not have access to this project")` when `user.role != Role.LEADER` and user is not in `project_members`.
  - `hasProjectAccess(Long projectId, Long userId, Role role)`: Centralized boolean helper returning `role == Role.LEADER || projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)` for conditional composition.
- **Refactored 9 Backend Services**:
  1. `MilestoneService.java`: Replaced private `verifyProjectAccess` with `projectAccessGuard.verifyProjectAccess(projectId, currentUsername)`.
  2. `ProjectService.java`: Replaced checks in `getProjectById` and `getProjectMembers` with `projectAccessGuard.verifyProjectAccess`. Kept `assignMembers` pre-insert check intact.
  3. `SectionService.java`: Injected `ProjectAccessGuard`, delegated in `verifyProjectAccess`, removed unused imports.
  4. `ExcelService.java`: Deleted private `verifyProjectAccess`, replaced 4 call sites (`validateImport`, `confirmImport`, `generateTemplate`, `exportCases`), and added session-project match check in `confirmImport()`:
     ```java
     if (!session.getProject().getId().equals(projectId)) {
         throw new ForbiddenException("Import session does not belong to project " + projectId);
     }
     ```
  5. `TestCaseService.java`: Injected `ProjectAccessGuard`, delegated in `verifyProjectAccess`, removed `ProjectMemberRepository`.
  6. `DashboardService.java`: Injected `ProjectAccessGuard`, replaced manual membership and user check in `getDashboard`.
  7. `TestRunService.java`: Injected `ProjectAccessGuard`, replaced in `verifyProjectAccess` and `addCasesToRunInternal` for assigned tester.
  8. `ExecutionService.java`: Injected `ProjectAccessGuard`, replaced in `verifyRunCaseAccess` and `verifyExecutionHistoryAccess`.
  9. `AttachmentService.java`: Injected `ProjectAccessGuard`, replaced in `verifyEntityAccess` while preserving creator bypass (`isCreator || projectAccessGuard.hasProjectAccess(...)`).
- **Unit Testing**:
  - Created `ProjectAccessGuardUnitTest.java` (7 test cases testing Leader bypass, member pass, non-member 403, user not found, and boolean helper).
  - Updated `ExcelServiceUnitTest.java` with `ProjectAccessGuard` integration and added `testConfirmImport_ThrowsForbiddenWhenSessionProjectMismatch()`.
- **Documentation**:
  - `CLAUDE.md`: Added Rule 23 for `ProjectAccessGuard`.
  - `CURRENT_STATE.md`: Added AD-27 detailing Access Control Consolidation and Confirm Import project guard; updated Section 6.

## 4. Files Changed
- `backend/src/main/java/com/testhub/testflowlite/project/ProjectAccessGuard.java` (New)
- `backend/src/main/java/com/testhub/testflowlite/milestone/MilestoneService.java`
- `backend/src/main/java/com/testhub/testflowlite/project/ProjectService.java`
- `backend/src/main/java/com/testhub/testflowlite/section/SectionService.java`
- `backend/src/main/java/com/testhub/testflowlite/excel/ExcelService.java`
- `backend/src/main/java/com/testhub/testflowlite/testcase/TestCaseService.java`
- `backend/src/main/java/com/testhub/testflowlite/dashboard/DashboardService.java`
- `backend/src/main/java/com/testhub/testflowlite/testrun/TestRunService.java`
- `backend/src/main/java/com/testhub/testflowlite/execution/ExecutionService.java`
- `backend/src/main/java/com/testhub/testflowlite/attachment/AttachmentService.java`
- `backend/src/test/java/com/testhub/testflowlite/project/ProjectAccessGuardUnitTest.java` (New)
- `backend/src/test/java/com/testhub/testflowlite/excel/ExcelServiceUnitTest.java`
- `CLAUDE.md`
- `CURRENT_STATE.md`
- `TASK_HANDOFF.md`

## 5. Validation Performed
- **Grep Verification**: `grep -rn "existsByProjectIdAndUserId" backend/src/main/java` returns exactly 2 occurrences: `ProjectAccessGuard.java` (central guard) and `ProjectService.java` (assign members check).
- **Backend Unit Tests**: `mvn test -Dtest=ProjectAccessGuardUnitTest,ExcelServiceUnitTest` → 13/13 tests PASS.
- **Backend Compilation**: `mvn test-compile` → BUILD SUCCESS (0 errors).
- **Frontend Build**: `npm run build` in `frontend/` → built in 53s, 0 TypeScript errors.

## 6. Known Issues / Blockers
- None. (Testcontainers integration suite requires local Docker daemon when executed).

## 7. Decisions Made
- `ProjectAccessGuard` provides both `verifyProjectAccess` (throws `ForbiddenException`) and `hasProjectAccess` (returns `boolean`) to accommodate both standard endpoints and multi-condition authorization logic (`AttachmentService` creator check, `TestRunService` assigned user input validation).

## 8. Explicit Next Step
- Commit changes with message: `fix(security): consolidate project access checks into ProjectAccessGuard and add confirmImport session-project match guard`
- Phase 2 Planning / Advanced QA features.

## 9. Context Files to Re-Read
- [CLAUDE.md](./CLAUDE.md)
- [CURRENT_STATE.md](./CURRENT_STATE.md)
- [AI_CONTEXT.md](./AI_CONTEXT.md)
