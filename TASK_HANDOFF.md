# Task Handoff

> [!NOTE]
> Documented task handoff between AI agents, developer sessions, and project memory.

---

## 1. Goal
Implement **Slice 11b: Security Hardening — Stop Leaking Exception Details**:
1. Update `GlobalExceptionHandler.handleGenericException` to log unhandled exceptions with full stack traces using `log.error("Unhandled exception", ex)` and return a safe generic message (`"An unexpected error occurred. Please try again or contact support."`) without concatenating `ex.getMessage()` into client responses.
2. Remove `server.error.include-message: always` from base `application.yml` (falling back to Spring Boot's default `never` for production and non-dev profiles).
3. Add `server.error.include-message: always` to `application-dev.yml` for local developer convenience.

## 2. Current Branch & Status
- **Branch**: `main`
- **Status**: Complete & Verified (29/29 unit tests pass, `npm run build` pass).

## 3. Work Completed
- **`GlobalExceptionHandler.java` (`com.testhub.testflowlite.common`)**:
  - Added `@Slf4j` and updated `handleGenericException(Exception ex)`:
    ```java
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again or contact support."));
    }
    ```
- **`application.yml`**:
  - Cleaned `server:` block to remove `error.include-message: always`.
- **`application-dev.yml`**:
  - Added `server.error.include-message: always` strictly scoped to the `dev` profile.
- **`GlobalExceptionHandlerUnitTest.java` (New)**:
  - Created unit tests verifying `handleGenericException` does not leak sensitive exception details (SQL error messages, table names, usernames) and returns the fixed generic message, as well as testing `handleNotFound` and `handleForbidden`.
- **Documentation**:
  - `CLAUDE.md`: Added Rule 25.
  - `CURRENT_STATE.md`: Added AD-30 and updated Section 6 next priorities.

## 4. Files Changed
- `backend/src/main/java/com/testhub/testflowlite/common/GlobalExceptionHandler.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-dev.yml`
- `backend/src/test/java/com/testhub/testflowlite/common/GlobalExceptionHandlerUnitTest.java` (New)
- `CLAUDE.md`
- `CURRENT_STATE.md`
- `TASK_HANDOFF.md`

## 5. Validation Performed
- **Grep Verification**: `grep -rn "An unexpected error occurred: \"" backend/src/main` → **0 matches** (confirms the string-concatenation leak pattern is gone).
- **Unit Tests**: `mvn test -Dtest=GlobalExceptionHandlerUnitTest,JwtTokenProviderUnitTest,ProjectAccessGuardUnitTest,ExcelServiceUnitTest,DashboardServiceUnitTest` → **29/29 unit tests PASS** (0 failures, 0 errors).
- **Frontend Build**: `npm run build` in `frontend/` → **Passed** (0 TypeScript errors, bundle generated in 54.46s).

## 6. Known Issues / Blockers
- **Testcontainers Integration Tests**: Running the full `mvn clean test` fails at container startup because the local Windows Docker daemon is not active (`Could not find a valid Docker environment`). All business and security logic is verified via the Mockito unit test suite (29/29 passing).

## 7. Decisions Made
- All specific domain exception handlers (`ResourceNotFoundException`, `ForbiddenException`, `MethodArgumentNotValidException`, etc.) were left intact because they produce intentional, validated user-facing messages. Only the catch-all `Exception.class` handler is sanitized to prevent unvetted exception messages from reaching the client.

## 8. Explicit Next Step
- Commit changes with message: `fix(security): stop leaking exception details in generic error handler, gate include-message to dev profile`
- Proceed to Slice 11c: CORS restriction, HTTP-only Cookie migration for JWT Refresh Token, and password complexity enforcement.

## 9. Context Files to Re-Read
- [CLAUDE.md](./CLAUDE.md)
- [CURRENT_STATE.md](./CURRENT_STATE.md)
- [AI_CONTEXT.md](./AI_CONTEXT.md)
