# Task Handoff

> [!NOTE]
> Documented task handoff between AI agents, developer sessions, and project memory.

---

## 1. Goal
Implement **Slice 10b (Hotfix): Restore Project-Existence Check in `ProjectAccessGuard.verifyProjectAccess`**:
- Restore centralized project existence validation (`projectRepository.existsById(projectId) -> 404 ResourceNotFoundException`) in `ProjectAccessGuard.verifyProjectAccess`.
- Fix the regression where `DashboardService.getDashboard`, `ExcelService.generateTemplate`, and `ExcelService.validateImport` lost existence validation for non-existent `projectId`s when invoked by Leaders or Testers.

## 2. Current Branch & Status
- **Branch**: `main`
- **Status**: Complete & Verified (22/22 unit tests pass, `npm run build` pass).

## 3. Work Completed
- **`ProjectAccessGuard.java` (`com.testhub.testflowlite.project`)**:
  - Injected `ProjectRepository`.
  - Added project existence check in `verifyProjectAccess(Long projectId, String currentUsername)`:
    ```java
    if (!projectRepository.existsById(projectId)) {
        throw new ResourceNotFoundException("Project not found: " + projectId);
    }
    ```
  - Left `hasProjectAccess` unchanged (zero redundant overhead for hot paths on already-resolved project entities).
- **Unit Tests**:
  - `ProjectAccessGuardUnitTest.java`: Added existence short-circuit test (`testVerifyProjectAccess_ProjectNotFoundThrowsResourceNotFound`) asserting `userRepository` and `projectMemberRepository` are not called, plus test verifying `hasProjectAccess` does not query `ProjectRepository`. (9 tests pass)
  - `ExcelServiceUnitTest.java`: Updated to 3-arg `ProjectAccessGuard` constructor; added `testGenerateTemplate_NonExistentProject_ThrowsResourceNotFound` and `testValidateImport_NonExistentProject_ThrowsResourceNotFound` asserting session is never saved. (8 tests pass)
  - `DashboardServiceUnitTest.java` (New): Created Mockito unit test suite verifying `getDashboard` behavior across Leader, Tester member, Tester non-member (403), and non-existent project (404 for both Leader and Tester). (5 tests pass)
- **Documentation**:
  - `CURRENT_STATE.md`: Added AD-28 detailing hotfix and scope rationale.

## 4. Files Changed
- `backend/src/main/java/com/testhub/testflowlite/project/ProjectAccessGuard.java` (Single production code file)
- `backend/src/test/java/com/testhub/testflowlite/project/ProjectAccessGuardUnitTest.java`
- `backend/src/test/java/com/testhub/testflowlite/excel/ExcelServiceUnitTest.java`
- `backend/src/test/java/com/testhub/testflowlite/dashboard/DashboardServiceUnitTest.java` (New)
- `backend/src/test/java/com/testhub/testflowlite/dashboard/DashboardControllerIntegrationTest.java`
- `backend/src/test/java/com/testhub/testflowlite/excel/ExcelControllerIntegrationTest.java`
- `CURRENT_STATE.md`
- `TASK_HANDOFF.md`

## 5. Validation Performed
- **Constructor Audit**: `grep -rn "new ProjectAccessGuard(" backend/src` → Exactly 2 manual test instantiation sites (`ExcelServiceUnitTest`, `DashboardServiceUnitTest`), both using 3-arg constructor `(projectRepository, projectMemberRepository, userRepository)`.
- **Unit Test Suite**: `mvn test -Dtest=ProjectAccessGuardUnitTest,ExcelServiceUnitTest,DashboardServiceUnitTest` → **22/22 tests PASS** (0 failures, 0 errors).
- **Frontend Build**: `npm run build` in `frontend/` → **Built in 14.90s, 0 TypeScript errors**.

## 6. Known Issues / Blockers
- **Testcontainers Integration Tests**: Running the full `mvn clean test` fails during container startup because the local Windows Docker daemon is not active / cannot find a valid Docker environment (`IllegalStateException: Could not find a valid Docker environment`). All business logic and regression test cases are covered and verified via Mockito unit tests.

## 7. Decisions Made
- `ProjectAccessGuard.verifyProjectAccess` performs the `projectRepository.existsById(projectId)` check upfront before resolving user or checking permissions, ensuring HTTP 404 is thrown consistently regardless of whether the requester is a Leader or a non-member Tester.
- `hasProjectAccess` intentionally omits the check since callers already have the project entity resolved via JPA relationships.

## 8. Explicit Next Step
- Commit changes with message: `fix(security): restore project-existence check in ProjectAccessGuard.verifyProjectAccess`

## 9. Context Files to Re-Read
- [CLAUDE.md](./CLAUDE.md)
- [CURRENT_STATE.md](./CURRENT_STATE.md)
- [AI_CONTEXT.md](./AI_CONTEXT.md)
