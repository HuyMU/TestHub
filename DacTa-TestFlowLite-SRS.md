# ĐẶC TẢ YÊU CẦU PHẦN MỀM (SRS)
## Dự án: TestFlow Lite — Hệ thống quản lý & thực thi Test Case

**Phiên bản:** 3.1
**Loại dự án:** Cá nhân (Personal Project)
**Đối tượng sử dụng:** Nhóm nhỏ dưới 10 người

### Changelog
| Version | Thay đổi chính |
|---|---|
| **v3.1** | Đổi thiết kế Import/Export Excel: 1 sheet duy nhất → sheet-per-root-section, cột "Section Path" (dấu `/`) → "Subsection Path" (dấu `>`), để phù hợp thực tế triển khai và dễ export theo từng Section lớn. |
| **v3.0** | Bỏ hẳn **Test Plan**; **Milestone** quay lại MVP dạng nhãn đơn giản; Ngôn ngữ mặc định **English**; Đổi **Folder → Section / Subsection**; **Tester được phép Import Excel**; Thêm **Test Data** & loại **Usability**; Đơn giản hóa trạng thái Test Case (**Draft / Review / Ready**); Hệ thống chỉ có **1 Leader duy nhất**. |

---

## 1. GIỚI THIỆU

### 1.1 Mục đích
Web app quản lý & thực thi Test Case (Manual + Automation) cho 1 nhóm nhỏ do 1 Leader duy nhất điều hành, có quy trình duyệt Test Case đơn giản, hỗ trợ Import/Export Excel.

### 1.2 Phạm vi dự án
Team < 10 người (1 Leader + tối đa ~9 Tester), nhiều Project, ngôn ngữ mặc định tiếng Anh.

### 1.3 Vai trò (Roles)

| Vai trò | Số lượng | Mô tả |
|---|---|---|
| **Leader** | Duy nhất 1 tài khoản (seed sẵn khi setup hệ thống) | Tạo Project, tạo/quản lý tài khoản Tester, quản lý Section, **duyệt Test Case**, tạo Test Run & Milestone, **review Test Result**, xem toàn bộ Dashboard/Report |
| **Tester** | Nhiều, được Leader gán vào từng Project | Thiết kế Test Case (tạo/sửa/submit để duyệt), **Import Excel**, thực thi Test Run được giao |

### 1.4 Thuật ngữ
- **Section**: Nút trong cây phân cấp dùng để tổ chức Test Case trong 1 Project. Một Section có thể chứa nhiều **Subsection** con (không giới hạn độ sâu).
- **Test Case**: Đơn vị kiểm thử, có trạng thái duyệt: `Draft` → `Review` → `Ready`.
- **Test Run**: Một lần thực thi tập hợp Test Case, gắn trực tiếp vào Project, có thể gắn tùy chọn 1 **Milestone**.
- **Milestone**: Nhãn đơn giản (tên + hạn chót) để nhóm/theo dõi tiến độ các Test Run.

---

## 2. TECH STACK

| Thành phần | Công nghệ |
|---|---|
| Backend | Java 17+, Spring Boot 3.x, Spring Security, Spring Data JPA |
| Database | MySQL 8 |
| Authentication | JWT (Access Token + Refresh Token) |
| Frontend | ReactJS (Vite), Ant Design, Axios, React Router |
| Excel Handling | Apache POI |
| API Docs | springdoc-openapi (Swagger UI) |
| Ngôn ngữ giao diện | Tiếng Anh (mặc định, MVP). Kiến trúc chuẩn bị sẵn cho i18n (VD: `react-i18next` + resource JSON theo key) để thêm tiếng Việt ở Phase 2 mà không cần sửa cấu trúc code |
| File Storage | Local filesystem (`/uploads`) cho MVP |
| Deployment | Docker Compose (App + MySQL) cho MVP/demo. **Kiến trúc deploy production (cloud VPS, CI/CD, domain/SSL) sẽ thiết kế riêng ở giai đoạn sau**, ngoài phạm vi đặc tả này |
| Real-time | Chưa áp dụng ở MVP (refresh thủ công). Quyết định dùng WebSocket/SSE hay không sẽ phụ thuộc vào môi trường deploy thực tế, để sau |
| API Auth cho Automation | API Token riêng (không dùng JWT user) |

### Kiến trúc tổng thể
```
[React SPA] --REST API/JWT--> [Spring Boot Backend] --JPA--> [MySQL]
                                     |
                              [Apache POI: Import/Export Excel]
                                     |
                        [REST API riêng cho Automation - API Token]
```

---

## 3. MA TRẬN PHÂN QUYỀN

| Chức năng | Leader | Tester |
|---|:---:|:---:|
| Quản lý tài khoản Tester | ✅ | ❌ |
| Tạo/Xóa Project | ✅ | ❌ |
| Gán Tester vào Project | ✅ | ❌ |
| CRUD Section/Subsection | ✅ | ⚠️ Tạo/sửa (không xóa) |
| Tạo/Sửa Test Case | ✅ | ✅ (case do mình tạo, khi ở trạng thái Draft) |
| **Import Excel** | ✅ | ✅ |
| **Export Excel** | ✅ | ✅ |
| **Submit Test Case để duyệt** | — | ✅ |
| **Duyệt Test Case (Review → Ready / trả về Draft)** | ✅ | ❌ |
| Tạo Test Run & Milestone | ✅ | ❌ |
| Thực thi Test (Execution) | ✅ | ✅ (case được giao) |
| **Review Test Result** | ✅ | ❌ |
| Xem Dashboard/Report | ✅ (toàn bộ) | ✅ (project được gán) |
| Tạo API Token (automation) | ✅ | ❌ |

---

## 4. QUY TRÌNH NGHIỆP VỤ CHÍNH

### 4.1 Vòng đời Test Case
```
[Tester tạo mới / Import Excel] → DRAFT
        ↓ (Tester bấm "Submit for Review")
      REVIEW
        ↓                              ↓
 (Leader duyệt "Ready")         (Leader từ chối, kèm comment)
        ↓                              ↓
      READY   <──────────────────  quay lại DRAFT (Tester sửa & submit lại)
```
**Quy tắc:**
- Test Case import từ Excel luôn ở trạng thái **Draft** (dù người import là Leader hay Tester) — đảm bảo mọi case đều qua bước duyệt.
- Chỉ Test Case ở trạng thái **Ready** mới hiển thị mặc định khi Leader tạo Test Run (có tùy chọn hiển thị cả case Draft/Review nếu cần gấp).
- Nếu Tester sửa nội dung case đã **Ready** → tự động chuyển về **Draft** (cần duyệt lại). Leader tự sửa thì giữ nguyên trạng thái.

### 4.2 Review Test Result
```
[Tester thực thi Run] → Kết quả: Passed/Failed/Blocked/Retest/Untested → is_reviewed = false
                                       ↓
                       [Leader xem lại kết quả trong Run]
                          ↓                        ↓
                  Đánh dấu "Reviewed"      "Request Retest" (kèm comment)
```

---

## 5. DANH SÁCH TÍNH NĂNG CHI TIẾT (Functional Requirements)

### 5.1 Authentication & User Management
- FR-01: Đăng nhập username/email + password (JWT).
- FR-02: Tài khoản Leader được seed sẵn khi khởi tạo hệ thống (không tạo qua UI).
- FR-03: Leader tạo/sửa/khóa tài khoản Tester.
- FR-04: User đổi mật khẩu cá nhân.
- FR-05: Leader gán Tester vào Project.
- FR-06: Leader sinh API Token cho automation.

### 5.2 Project Management
- FR-07: CRUD Project (name, description, status: Active/Archived) — chỉ Leader.
- FR-08: Danh sách Project hiển thị theo quyền: Leader thấy tất cả, Tester chỉ thấy Project được gán.

### 5.3 Section / Subsection
- FR-09: Tạo cây Section phân cấp không giới hạn độ sâu (Section → Add Subsection) trong từng Project.
- FR-10: Tester tạo/sửa Section/Subsection; chỉ Leader được xóa.

### 5.4 Test Case Management

**Các trường của Test Case:**

| Trường | Bắt buộc | Ghi chú |
|---|:---:|---|
| ID | Tự sinh | VD: `TC-0001` |
| Title | ✅ | |
| Pre-condition | ✅ | |
| Steps | ✅ | |
| Expected Result | ✅ | |
| Test Data | ❌ | Dữ liệu đầu vào dùng khi test (VD: tài khoản test, input mẫu) |
| Priority | ✅ | Low / Medium / High / Critical |
| Type | ✅ | Functional / Regression / Smoke / Performance / Security / **Usability** / Other |
| Automation Status | ❌ | Manual / Automated / To Automate (mặc định Manual) |
| Status | Tự quản lý theo workflow | **Draft / Review / Ready** |

- FR-11: CRUD Test Case với đầy đủ các trường trên.
- FR-12: Workflow trạng thái theo mục 4.1 (Submit for Review, Leader duyệt Ready/trả về Draft kèm comment).
- FR-13: Màn hình **Review Queue**: danh sách case đang "Review", Leader duyệt từng case hoặc hàng loạt.
- FR-14: Clone/Duplicate Test Case (case mới ở trạng thái Draft).
- FR-15: Tìm kiếm & lọc theo: Section, Priority, Type, Status, Automation Status, từ khóa.
- FR-16: Audit log: ai sửa/duyệt/từ chối, khi nào.

### 5.5 Import / Export Excel (chi tiết mục 8)
- FR-17: **Cả Leader và Tester** import Test Case từ Excel theo template chuẩn (sheet-per-root-section) — case import vào luôn ở trạng thái Draft.
- FR-18: Bước Validate/Preview trước khi Confirm Import, báo lỗi theo từng dòng.
- FR-19: Export Test Case ra Excel (toàn bộ/theo filter) đúng format template — cả 2 vai trò đều thực hiện được.
- FR-20: Export kết quả Test Run ra Excel.

### 5.6 Test Run, Milestone & Execution
- FR-21: Leader tạo Milestone (name, due date) thuộc về 1 Project.
- FR-22: Leader tạo Test Run, chọn Test Case (mặc định chỉ case Ready), tùy chọn gắn 1 Milestone.
- FR-23: Gán từng case trong Run cho 1 Tester cụ thể.
- FR-24: Tester thực thi: chọn kết quả (Passed/Failed/Blocked/Retest/Untested), comment, đính kèm file/ảnh, Defect Ref (text/link tự do).
- FR-25: Leader review kết quả (Reviewed / Request Retest kèm comment).
- FR-26: Đóng (Close) Run khi hoàn tất.
- FR-27: Dashboard hiển thị tiến độ theo Milestone (số Run/case đã hoàn thành trên tổng số).

### 5.7 Automation Result API
- FR-28: REST endpoint (xác thực API Token) nhận kết quả test tự động:
  ```json
  POST /api/automation/results
  {
    "run_id": 123,
    "case_ref": "TC-0045",
    "status": "failed",
    "duration_ms": 3200,
    "message": "AssertionError: expected true, got false",
    "executed_at": "2026-08-05T10:00:00Z"
  }
  ```
- FR-29: Kết quả tự động cập nhật vào Run, đánh dấu "Executed by: Automation", `is_reviewed = false`.

### 5.8 Dashboard & Report
- FR-30: Dashboard theo Project: tỷ lệ Pass/Fail/Blocked, số case đang chờ Review, tiến độ theo Milestone.
- FR-31: Report chi tiết 1 Test Run, export Excel.

---

## 6. YÊU CẦU PHI CHỨC NĂNG (NFR)

| Loại | Yêu cầu |
|---|---|
| Hiệu năng | < 10 user đồng thời, vài chục nghìn Test Case |
| Bảo mật | BCrypt hash mật khẩu, JWT có hạn + Refresh Token, kiểm tra quyền ở tầng Service |
| Ngôn ngữ | UI mặc định English; code/label chuẩn bị sẵn cấu trúc i18n để thêm tiếng Việt ở Phase 2 |
| Khả dụng (MVP) | Docker Compose, chạy local/VPS đơn giản |
| Khả dụng (tương lai) | Kiến trúc deploy production chính thức — thiết kế riêng sau, ngoài phạm vi bản đặc tả này |
| Sao lưu | Script/cron backup MySQL dump định kỳ |
| Ghi log | Audit log cho các hành động quan trọng |

---

## 7. THIẾT KẾ DỮ LIỆU (Data Model)

| Bảng | Mô tả |
|---|---|
| `users` | id, username, email, password_hash, full_name, role (LEADER/TESTER), is_active, created_at |
| `projects` | id, name, description, status, created_by, created_at |
| `project_members` | id, project_id, user_id (Tester được gán) |
| `sections` | id, project_id, parent_section_id (self-reference, nullable → Subsection), name, sort_order |
| `test_cases` | id, code (VD TC-0001), section_id, title, precondition, steps, expected_result, **test_data**, priority, **type** (bao gồm Usability), automation_status, **status** (Draft/Review/Ready), review_comment, created_by, reviewed_by, reviewed_at, created_at, updated_at |
| `milestones` | id, project_id, name, due_date, status (Open/Closed), created_by, created_at |
| `test_runs` | id, project_id, milestone_id (nullable), name, status, created_by, created_at, closed_at |
| `test_run_cases` | id, run_id, case_id, title, precondition, steps, expected_result, test_data (snapshot), assigned_to, result_status, executed_by, executed_at, comment, defect_ref, is_reviewed, reviewed_by, reviewed_at, review_comment |
| `execution_history` | id, run_case_id, result_status, comment, executed_by, executed_at |
| `attachments` | id, entity_type, entity_id, file_path, uploaded_by, uploaded_at |
| `api_tokens` | id, created_by, token_hash, revoked_at, created_at, last_used_at |
| `audit_logs` | id, user_id, action, entity_type, entity_id, detail_json, created_at |
| `excel_import_sessions` | id, import_session_id, project_id, created_by, parsed_payload_json, error_lines_json, expires_at, created_at |

### Quan hệ chính
```
projects 1--n project_members n--1 users (Tester)
projects 1--n sections (self-reference parent → subsection) 1--n test_cases
projects 1--n milestones
projects 1--n test_runs n--1 milestones (nullable)
test_runs 1--n test_run_cases n--1 test_cases
test_run_cases 1--n execution_history
```

---

## 8. ĐẶC TẢ IMPORT/EXPORT EXCEL

### 8.1 Cấu trúc Template Import / Export (Sheet-per-Root-Section)
Mỗi worksheet (sheet) trong file `.xlsx` đại diện cho **1 Section gốc (root Section)** trong Project. Tên sheet = Tên root Section (tối đa 31 ký tự).

| Cột | Tên Cột | Bắt buộc | Kiểu dữ liệu | Ghi chú |
|---|---|:---:|---|---|
| A | Subsection Path | ❌ | Text | Phân cấp bằng `>` (VD: `Parent > Child > Grandchild`). Rỗng = thuộc trực tiếp root Section |
| B | Title | ✅ | Text | Tiêu đề Test Case |
| C | Precondition | ✅ | Text | Điều kiện tiên quyết |
| D | Steps | ✅ | Text | Các bước thực hiện multi-line với đánh số linh hoạt (`1.`, `Step 1:`) |
| E | Expected Result | ✅ | Text | Kết quả kỳ vọng tương ứng các bước |
| F | Test Data | ❌ | Text | Dữ liệu kiểm thử / tài khoản test |
| G | Priority | ❌ | Enum | `Low` / `Medium` / `High` / `Critical` (Mặc định: `Medium`) |
| H | Type | ❌ | Enum | `Functional` / `Regression` / `Smoke` / `Performance` / `Security` / `Usability` / `Other` (Mặc định: `Functional`) |
| I | Automation Status | ❌ | Enum | `Manual` / `Automated` / `To Automate` (Mặc định: `Manual`) |

> Tất cả Test Case khi import vào đều tự động ở trạng thái **Draft** — áp dụng cho cả Leader và Tester. Section hoặc Subsection chưa tồn tại sẽ được hệ thống tự động khởi tạo khi Confirm Import.

### 8.2 Quy trình Import (2 bước)
1. **Validate/Preview** (`POST /api/projects/{projectId}/cases/import/validate`): Kiểm tra định dạng text, required fields, enum hợp lệ và tương ứng bước số. Trả về `importSessionId` và danh sách preview lỗi dòng — chưa ghi DB.
2. **Confirm Import** (`POST /api/projects/{projectId}/cases/import/confirm`): Xác nhận `importSessionId`, tự động tạo Section/Subsection còn thiếu, ghi DB toàn bộ Test Case ở trạng thái `Draft`.

### 8.3 Export
- **Export Test Case**: đúng format Sheet-per-Root-Section (`.xlsx`) + tự động ẩn cột Test Data / Automation Status nếu tất cả case trong sheet mang giá trị mặc định.
- **Export Test Run Result**: thêm `Result Status`, `Is Reviewed`, `Executed By`, `Executed At`, `Comment`, `Defect Ref`.

---

## 9. THIẾT KẾ API (Danh sách endpoint chính)

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/auth/login`, `/api/auth/refresh` | Đăng nhập / refresh token |
| GET | `/api/users/me` | Lấy thông tin user hiện tại |
| PUT | `/api/users/me/password` | Đổi mật khẩu cá nhân |
| GET/POST/PUT | `/api/users` | Quản lý Tester (Leader) |
| GET/POST/PUT | `/api/projects` | Quản lý Project |
| POST/DELETE | `/api/projects/{id}/members` | Gán / Xóa Tester trong Project |
| GET/POST/PUT/DELETE | `/api/projects/{id}/sections` | Quản lý Section/Subsection |
| GET/POST/PUT/DELETE | `/api/cases` | CRUD Test Case |
| POST | `/api/cases/{id}/submit-review` | Tester submit để duyệt |
| POST | `/api/cases/{id}/approve` | Leader duyệt → Ready |
| POST | `/api/cases/{id}/reject` | Leader từ chối → về Draft (kèm comment) |
| POST | `/api/cases/{id}/clone` | Clone Test Case |
| GET | `/api/cases/review-queue` | Danh sách case đang Review |
| POST | `/api/projects/{projectId}/cases/import/validate` | Step 1: Validate Excel template |
| POST | `/api/projects/{projectId}/cases/import/confirm` | Step 2: Confirm import vào DB |
| GET | `/api/projects/{projectId}/cases/import/template` | Download file mẫu Import (.xlsx) |
| GET | `/api/projects/{projectId}/cases/export` | Export Excel |
| GET/POST/PUT/DELETE | `/api/projects/{projectId}/milestones` | Quản lý Milestone (Leader) |
| GET/POST | `/api/projects/{projectId}/runs` | Tạo & xem danh sách Test Run |
| GET | `/api/runs/{id}` | Chi tiết Test Run + danh sách case (snapshot) |
| POST | `/api/runs/{id}/cases` | Thêm case vào Run (open run) |
| DELETE | `/api/runs/{id}/cases/{runCaseId}` | Gỡ case khỏi Run (open run) |
| POST | `/api/runs/{id}/close` | Đóng Test Run |
| POST | `/api/runs/{id}/cases/{caseId}/execute` | Ghi nhận kết quả thực thi |
| POST | `/api/runs/{id}/cases/{caseId}/review` | Leader review kết quả |
| POST | `/api/automation/results` | Nhận kết quả automation (API Token) |
| GET | `/api/runs/{id}/report` | Report/Export Run |
| GET | `/api/dashboard/{projectId}` | Dữ liệu Dashboard |

---

## 10. DANH SÁCH MÀN HÌNH (UI Screens)

1. Login
2. Dashboard (theo Project)
3. Danh sách Project (Leader)
4. Chi tiết Project — Tabs: Sections/Cases | Runs | Milestones | Members
5. Cây Section/Subsection + danh sách Test Case (filter theo Status, Priority, Type)
6. Form Tạo/Sửa Test Case (đầy đủ 10 trường, nút Submit for Review)
7. Review Queue (Leader): duyệt case Review → Ready/Draft
8. Wizard Import Excel (dùng chung cho Leader & Tester)
9. Quản lý Milestone
10. Tạo Test Run (chọn case Ready, gắn Milestone tùy chọn)
11. Màn hình Thực thi Test (Tester)
12. Review Result (Leader)
13. Report chi tiết Run (Export Excel)
14. Quản lý Tester (Leader)
15. Quản lý API Token

---

## 11. LỘ TRÌNH TRIỂN KHAI

| Giai đoạn | Nội dung |
|---|---|
| **Phase 1 — MVP** | Auth, Project, Section/Subsection, Test Case (đầy đủ field + workflow Draft/Review/Ready), Import/Export Excel (Leader + Tester), Test Run + Milestone đơn giản, Execution + Result Review, Automation API, Dashboard |
| **Phase 2** | Đa ngôn ngữ (thêm tiếng Việt qua i18n), thiết kế deploy production chính thức, cân nhắc real-time (WebSocket) tùy theo hạ tầng, Email/In-app Notification, Report nâng cao (PDF) |
| **Phase 3 (tùy chọn, nếu cần)** | Webhook CI/CD, tích hợp bug-tracker thật |

---

## 12. RỦI RO & GIẢ ĐỊNH

- **Milestone thuộc phạm vi 1 Project** (giả định — nếu anh/chị muốn Milestone dùng chung nhiều Project thì cần điều chỉnh `milestones.project_id` thành nullable).
- Reject Test Case không có state riêng, quay thẳng về Draft — nếu cần phân biệt rõ "Draft mới tạo" và "Draft bị từ chối", có thể bổ sung cờ `was_rejected` sau.
- Không còn khái niệm "Deprecated" — case không dùng nữa thì đơn giản là không đưa vào Run mới; nếu cần ẩn hẳn khỏi danh sách, đây sẽ là điểm bổ sung ở Phase 2.
- File đính kèm lưu local filesystem (MVP).
- Giả định chỉ 1 Leader trong suốt vòng đời hệ thống; nếu sau này cần thêm Leader thứ 2, cần thao tác trực tiếp ở DB/seed script (không có UI).

---

*Hết tài liệu.*
