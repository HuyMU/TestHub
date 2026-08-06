# System Architecture Overview — TestFlow Lite

## 1. High-Level Architecture

TestFlow Lite (TestHub) follows a clean client-server architecture with a single Single Page Application (SPA) frontend and a monolithic Spring Boot backend.

```
+-----------------------------------------------------------------------+
|                              FRONTEND                                 |
|         ReactJS (Vite) + Ant Design 5.x + React Router v6             |
+-----------------------------------+-----------------------------------+
                                    |
                    REST API / JWT  |  REST API / API Token (Automation)
                                    v
+-----------------------------------------------------------------------+
|                              BACKEND                                  |
|         Java 17 + Spring Boot 3.x (Package-by-Feature)                |
|  - Spring Security (JWT Provider & Filter)                            |
|  - Spring Data JPA                                                    |
|  - Apache POI (Excel Validation & Generator)                          |
|  - springdoc-openapi (Swagger UI)                                     |
+-----------------------------------+-----------------------------------+
                                    |
                     JDBC / Flyway  |  Local Storage
                                    v
                 +------------------+-------------------+
                 | MySQL 8 Database | Local `/uploads`  |
                 +------------------+-------------------+
```

---

## 2. Package-by-Feature Architecture

The backend code is organized strictly by domain feature rather than by layer. This isolates features into self-contained packages:

```
com.testhub.testflowlite/
├── config/             # Global configurations (Security, CORS, OpenAPI, JWT)
├── common/             # Base Entity, Global Exception Handler, ApiResponse wrapper, Common Enums
├── security/           # Authentication mechanisms, JwtTokenProvider, JwtAuthFilter
├── auth/               # Login & Token Refresh endpoints
├── user/               # Tester user management by Leader
├── project/            # Project CRUD & Member assignments
├── section/            # Hierarchical tree of Sections & Subsections
├── testcase/           # Test Case management & 3-stage Workflow (Draft/Review/Ready)
├── excel/              # 2-step Apache POI Import Validation/Confirm & Export
├── milestone/          # Milestone management (name, due date)
├── testrun/            # Test Run creation & Tester case assignments
├── execution/          # Execution result reporting & Leader review
├── automation/         # Automation ingestion API (/api/automation/results with API token)
├── attachment/         # Local file attachment handling
├── audit/              # Audit logging hooks & queries
└── dashboard/          # Aggregated completion statistics & status metrics
```

---

## 3. Communication & Security Layers

1. **Authentication Framework**: User authentication uses JWT bearer tokens (`Authorization: Bearer <token>`). Short-lived access tokens combined with refresh tokens.
2. **Automation Authentication**: Automation test execution endpoints bypass user JWT and use a dedicated `X-API-TOKEN` header validated against the `api_tokens` table.
3. **Database Migration**: Flyway automatically runs database migrations on backend container initialization, bringing the MySQL 8 schema to the current version.
