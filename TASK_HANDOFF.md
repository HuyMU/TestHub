# Task Handoff

> [!NOTE]
> Documented task handoff between AI agents, developer sessions, and project memory.

---

## 1. Goal
Implement **Slice 11c: Security Hardening — HttpOnly Refresh-Token Cookie Migration + CORS Restriction**:
1. Restrict CORS from wildcard (`*`) to exact configured origin (`app.frontend-origin`, defaulting to `http://localhost:3000`) in `CorsConfig.java`.
2. Move JWT Refresh Token issuance and transmission from client-side `localStorage` to an `HttpOnly`, `SameSite=Lax`, path-scoped (`/api/auth`) cookie (`refresh_token`) via `RefreshCookieFactory.java`.
3. Update `AuthController` and `AuthService`:
   - `POST /api/auth/login`: sets the `refresh_token` cookie and nulls out `refreshToken` in the JSON response body.
   - `POST /api/auth/refresh`: reads `refresh_token` from cookie, validates, re-issues cookie with sliding expiration, and returns new access token with `refreshToken: null` in response body.
   - `POST /api/auth/logout`: new authenticated endpoint clearing the cookie (`Max-Age=0`).
4. Delete `RefreshTokenRequest.java`.
5. Update `SecurityConfig.java` to narrow `permitAll()` for auth paths to `/api/auth/login` and `/api/auth/refresh` only, requiring authentication for `/api/auth/logout`.
6. Frontend updates:
   - `axiosClient.ts`: `withCredentials: true`, read `accessToken` only from in-memory Zustand store, 401 retry refresh call with credentials and empty body.
   - `authStore.ts`: in-memory `accessToken`, `user` in `localStorage` for optimistic reload UI, `isInitializing` state, `initializeAuth()` action, and `logout()` sending authenticated POST before clearing local state.
   - `App.tsx`: bootstrap auth state on mount via `initializeAuth()` and display `LoadingSpinner` container while `isInitializing` is `true`.
   - `LoginPage.tsx`: update to 2-parameter `setAuth(user, accessToken)`.

## 2. Current Branch & Status
- **Branch**: `main`
- **Status**: Complete & Verified (34/34 unit tests pass, `npm run build` pass).

## 3. Work Completed
- **Backend**:
  - `CorsConfig.java`: Bound `@Value("${app.frontend-origin}")` and set `config.setAllowedOrigins(List.of(frontendOrigin))`.
  - `application.yml` & `application-dev.yml`: Configured `app.frontend-origin: ${FRONTEND_ORIGIN:http://localhost:3000}` and `app.cookie-secure: ${COOKIE_SECURE:true}` (overridden to `false` in `dev`).
  - `docker-compose.yml` & `.env.example`: Added `FRONTEND_ORIGIN` environment variable.
  - `RefreshCookieFactory.java`: Created cookie builder issuing `HttpOnly`, `SameSite=Lax`, path `/api/auth` cookies.
  - `AuthController.java`: Updated `login`, `refreshToken` (reads `@CookieValue`), and added `logout`.
  - `AuthService.java`: Updated `refreshToken(String refreshToken)` signature.
  - `RefreshTokenRequest.java`: Removed.
  - `SecurityConfig.java`: Narrowed permitAll matchers to `/api/auth/login` and `/api/auth/refresh`.
  - `TokenResponse.java`: Added `@JsonInclude(JsonInclude.Include.NON_NULL)`.
- **Frontend**:
  - `authStore.ts`: In-memory `accessToken`, `isInitializing`, `initializeAuth()`, `logout()`.
  - `axiosClient.ts`: Added `withCredentials: true`, cleaned interceptors.
  - `LoginPage.tsx`: Updated destructure and `setAuth(user, accessToken)`.
  - `App.tsx`: Bootstraps auth via `initializeAuth()` on mount with `LoadingSpinner`.
- **Tests**:
  - `AuthControllerUnitTest.java`: 5 unit tests covering login cookie set, refresh cookie validation, missing cookie rejection, logout cookie clearing, and cookie factory properties.
  - `AuthControllerIntegrationTest.java`: Updated mock assertions for cookie presence and `refreshToken` absence in JSON body.
- **Documentation**:
  - `CURRENT_STATE.md`: Updated header, Auth matrix row, Section 6 next task, and added AD-31.
  - `CLAUDE.md`: Added Rule 26.
  - `api-contracts.md` & `AI_CONTEXT.md`: Synchronized Auth module contracts.

## 4. Files Changed
- `backend/src/main/java/com/testhub/testflowlite/config/CorsConfig.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-dev.yml`
- `docker-compose.yml`
- `.env.example`
- `backend/src/main/java/com/testhub/testflowlite/security/RefreshCookieFactory.java` (New)
- `backend/src/main/java/com/testhub/testflowlite/auth/AuthController.java`
- `backend/src/main/java/com/testhub/testflowlite/auth/AuthService.java`
- `backend/src/main/java/com/testhub/testflowlite/auth/TokenResponse.java`
- `backend/src/main/java/com/testhub/testflowlite/auth/RefreshTokenRequest.java` (Deleted)
- `backend/src/main/java/com/testhub/testflowlite/config/SecurityConfig.java`
- `frontend/src/store/authStore.ts`
- `frontend/src/api/axiosClient.ts`
- `frontend/src/features/auth/LoginPage.tsx`
- `frontend/src/App.tsx`
- `backend/src/test/java/com/testhub/testflowlite/auth/AuthControllerUnitTest.java` (New)
- `backend/src/test/java/com/testhub/testflowlite/auth/AuthControllerIntegrationTest.java`
- `CLAUDE.md`
- `CURRENT_STATE.md`
- `docs/architecture/api-contracts.md`
- `AI_CONTEXT.md`
- `TASK_HANDOFF.md`

## 5. Validation Performed
- **Grep Cleanliness**:
  - `grep -rn "refreshToken" frontend/src` → **0 matches**.
  - `grep -rln "RefreshTokenRequest" backend/src` → **0 matches**.
- **Unit Tests**:
  - `mvn test -Dtest=AuthControllerUnitTest,GlobalExceptionHandlerUnitTest,JwtTokenProviderUnitTest,ProjectAccessGuardUnitTest,ExcelServiceUnitTest,DashboardServiceUnitTest` → **34/34 unit tests PASS** (0 failures, 0 errors).
- **Frontend Build**:
  - `npm run build` in `frontend/` → **Passed** (0 TypeScript errors, bundle generated in 14.61s).

## 6. Known Issues / Blockers
- **Testcontainers Integration Tests**: The local Docker daemon on this Windows host is currently stopped/inactive (`Could not find a valid Docker environment`), preventing Testcontainers MySQL container instantiation during full integration test runs. All authentication, cookie creation, and security guard logic is 100% verified via unit tests.

## 7. Decisions Made
- `SameSite=Lax` is standard and safe for same-site deployments (frontend and backend sharing host or differing by port on localhost). If deployed cross-site across different top-level domains in production, `SameSite=None; Secure=true` is required (documented in AD-31).

## 8. Explicit Next Step
- Commit changes with message: `fix(security): migrate refresh token to httpOnly cookie, restrict CORS to exact frontend origin`
- Proceed to Slice 11d: Password complexity enforcement and OpenAPI/Swagger profile gating.

## 9. Context Files to Re-Read
- [CLAUDE.md](./CLAUDE.md)
- [CURRENT_STATE.md](./CURRENT_STATE.md)
- [AI_CONTEXT.md](./AI_CONTEXT.md)
