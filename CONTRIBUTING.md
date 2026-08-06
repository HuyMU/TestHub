# Contributing Guidelines for TestFlow Lite

Thank you for contributing to **TestFlow Lite (TestHub)**. Please adhere to the following workflow and guidelines.

---

## 1. Branching Strategy

- `main`: Protected production-ready branch.
- `feature/<feature-name>`: New features (e.g., `feature/testcase-review-queue`).
- `bugfix/<bug-name>`: Bug fixes (e.g., `bugfix/excel-import-null-pointer`).
- `chore/<task-name>`: Infrastructure, CI, or documentation updates.

---

## 2. Commit Message Standards

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short summary>

[optional body]
```

### Types:
- `feat`: A new feature
- `fix`: A bug fix
- `docs`: Documentation only changes
- `style`: Code formatting, missing semi-colons (no code change)
- `refactor`: Refactoring without feature/fix change
- `test`: Adding or correcting tests
- `chore`: Build process or tool changes

---

## 3. Pre-Pull Request Checklist

Before submitting a Pull Request, ensure:

1. [ ] **Single Source of Truth Check**: Changes comply with [DacTa-TestFlowLite-SRS.md](./DacTa-TestFlowLite-SRS.md).
2. [ ] **Business Rules Verified**: Check all items in Section 7 of [CLAUDE.md](./CLAUDE.md) (e.g., single seed Leader, 3-state Test Case lifecycle, Excel import in `Draft` state).
3. [ ] **Backend Compilation**: `mvn clean test` completes successfully without errors in `backend/`.
4. [ ] **Frontend Build**: `npm run build` completes successfully without TypeScript errors in `frontend/`.
5. [ ] **API Specs**: Any new/updated REST APIs are documented with OpenAPI annotations.
6. [ ] **No Secret Leaks**: No `.env` files or secrets committed.
