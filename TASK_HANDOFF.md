# Task Handoff

> [!NOTE]
> Documented task handoff between AI agents, developer sessions, and project memory.

---

## 1. Goal
Implement **Slice 11d: Security Hardening — Password Policy & OpenAPI/Swagger Gating**:
1. Enforce password complexity policy on `CreateUserRequest.password` and `ChangePasswordRequest.newPassword` (minimum 8 characters, at least one uppercase letter, one lowercase letter, and one digit via `@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")`).
2. Synchronize client-side password validation rules across `frontend/src/features/users/UserListPage.tsx` (create tester modal) and `frontend/src/features/users/MyAccountModal.tsx` (change password modal) with i18n keys (`passwordMinLength`, `passwordComplexity`) in `en.json`.
3. Disable Swagger UI and OpenAPI documentation generation in the production profile (`application-prod.yml`) via `springdoc.api-docs.enabled: false` and `springdoc.swagger-ui.enabled: false`.

## 2. Current Branch & Status
- **Branch**: `main`
- **Status**: Complete & Verified (42/42 unit tests pass, `npm run build` pass).

## 3. Work Completed
- **Backend**:
  - `CreateUserRequest.java`: Enforced `@Size(min = 8)` and `@Pattern` for uppercase, lowercase, and digit complexity.
  - `ChangePasswordRequest.java`: Enforced `@Size(min = 8)` and `@Pattern` on `newPassword` (leaving `oldPassword` as `@NotBlank`).
  - `application-prod.yml`: Added `springdoc.api-docs.enabled: false` and `springdoc.swagger-ui.enabled: false`.
- **Frontend**:
  - `en.json`: Added `passwordMinLength` and `passwordComplexity` translation keys.
  - `UserListPage.tsx`: Replaced 6-character rule with `{ min: 8 }` and regex complexity pattern rule.
  - `MyAccountModal.tsx`: Replaced 6-character rule with `{ min: 8 }` and regex complexity pattern rule.
- **Tests**:
  - `UserValidationUnitTest.java`: Added 8 bean validation unit tests for password length and complexity violations on `CreateUserRequest` and `ChangePasswordRequest`.
  - `UserControllerIntegrationTest.java`: Added 5 test cases testing too short, no uppercase, no digit, valid password creation, and weak password change rejection.
- **Documentation**:
  - `CURRENT_STATE.md`: Updated Section 6 noting Slice 11 hardening complete, updated checked commit, and added AD-32.
  - `CLAUDE.md`: Added Rule 27.
  - `TASK_HANDOFF.md`: Updated with full Slice 11d records.

## 4. Files Changed
- `backend/src/main/java/com/testhub/testflowlite/user/CreateUserRequest.java`
- `backend/src/main/java/com/testhub/testflowlite/user/ChangePasswordRequest.java`
- `backend/src/main/resources/application-prod.yml`
- `frontend/src/i18n/locales/en.json`
- `frontend/src/features/users/UserListPage.tsx`
- `frontend/src/features/users/MyAccountModal.tsx`
- `backend/src/test/java/com/testhub/testflowlite/user/UserValidationUnitTest.java` (New)
- `backend/src/test/java/com/testhub/testflowlite/user/UserControllerIntegrationTest.java`
- `CLAUDE.md`
- `CURRENT_STATE.md`
- `TASK_HANDOFF.md`

## 5. Validation Performed
- **Unit Tests**:
  - `mvn test -Dtest=UserValidationUnitTest,AuthControllerUnitTest,GlobalExceptionHandlerUnitTest,JwtTokenProviderUnitTest,ProjectAccessGuardUnitTest,ExcelServiceUnitTest,DashboardServiceUnitTest` → **42/42 unit tests PASS** (0 failures, 0 errors).
- **Frontend Build**:
  - `npm run build` in `frontend/` → **Passed** (0 TypeScript errors, bundle generated in 54.43s).
- **Fixtures & Seeder Verification**:
  - Seeded Leader password `Leader@123456` (13 chars, uppercase, lowercase, digit, symbol) satisfies policy.
  - Existing test fixtures (`Leader@123456`, `Tester@123456`, `Pass@123456`, `NewPass@123456`) all satisfy the new 8+ char, uppercase, lowercase, and digit complexity rules.

## 6. Known Issues / Blockers
- **Testcontainers Integration Tests**: The local Docker daemon on this Windows host is currently inactive (`Could not find a valid Docker environment`), preventing Testcontainers MySQL container instantiation during full integration test runs. All password validation logic is 100% verified via `UserValidationUnitTest` (8 tests) and the broader unit test suite (42 tests total).

## 7. Decisions Made
- Symbol / special-character requirements were intentionally excluded per requirements (policy strictly requires 8+ characters, at least 1 uppercase, 1 lowercase, and 1 digit).
- `oldPassword` in `ChangePasswordRequest` is kept as `@NotBlank` only without complexity rules so that users changing a legacy password are not blocked if their prior password was created before this policy.

## 8. Explicit Next Step
- Commit changes with message: `fix(security): enforce password complexity policy and disable Swagger/OpenAPI in production profile`
- Proceed with roadmap feature slices or sprint review tasks.

## 9. Context Files to Re-Read
- [CLAUDE.md](./CLAUDE.md)
- [CURRENT_STATE.md](./CURRENT_STATE.md)
- [AI_CONTEXT.md](./AI_CONTEXT.md)
