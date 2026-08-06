# TestFlow Lite (TestHub)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.x-blue.svg)](https://reactjs.org/)
[![Ant Design](https://img.shields.io/badge/Ant%20Design-5.x-1890ff.svg)](https://ant.design/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

TestFlow Lite is a lightweight, high-efficiency Test Case management and execution system designed for small QA teams (< 10 members: 1 Leader + up to 9 Testers). It provides streamlined Test Case workflows (Draft → Review → Ready), 2-step Excel Import/Export, Test Run & Milestone tracking, Automation API results integration, and Project Dashboards.

## 🚀 Quick Start (Local Development)

### Prerequisites
- [Docker](https://www.docker.com/) & [Docker Compose](https://docs.docker.com/compose/)
- [Java 17+](https://adoptium.net/) (for local backend dev)
- [Node.js 18+](https://nodejs.org/) & `npm` (for local frontend dev)

### Run with Docker Compose
1. Clone the repository and copy the environment template:
   ```bash
   cp .env.example .env
   ```
2. Launch database and backend services:
   ```bash
   docker-compose up -d --build
   ```
3. Access services:
   - Backend REST API & Swagger UI: `http://localhost:8080/swagger-ui.html`
   - Frontend SPA: `http://localhost:3000` (or `http://localhost:5173` via Vite dev)

Default seed Leader account (created on first application launch):
- Username / Email: `leader` / `leader@testhub.com`
- Default password set in `.env` (`Leader@123456`)

---

## 📁 Repository Structure Summary

```
TestHub/
├── docs/                   # Architecture, ERD, API specs, ADR, screen catalog
├── backend/                # Java 17, Spring Boot 3.x REST API (package-by-feature)
├── frontend/               # React (Vite + TypeScript + Ant Design) SPA
├── scripts/                # Database backup script
├── .github/workflows/      # GitHub Actions CI workflow
├── docker-compose.yml      # Containerized deployment orchestration
├── CLAUDE.md               # AI Agent operating rules & non-negotiables
└── AI_CONTEXT.md           # Domain reference, data models, state machines & API matrix
```

For complete architectural details, see [docs/architecture/overview.md](./docs/architecture/overview.md).

---

## 📚 Key Documentation

- [DacTa-TestFlowLite-SRS.md](./DacTa-TestFlowLite-SRS.md) — Single Source of Truth (Software Requirements Specification v3.0)
- [CLAUDE.md](./CLAUDE.md) — Guidelines & non-negotiable business rules for AI/human developers
- [AI_CONTEXT.md](./AI_CONTEXT.md) — Rapid domain reference, ERD diagram, state machines, and API catalog
- [CONTRIBUTING.md](./CONTRIBUTING.md) — Git workflow, branch conventions, and pre-PR checklist