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

---

## 3. QUẢN LÝ NGƯỜI DÙNG & XÁC THỰC

- Gồm **1 Leader duy nhất** (seeded sẵn) và các **Tester** do Leader tạo.
- Leader có thể tạo/sửa thông tin (full name, email, active status) của Tester. KHÔNG có giao diện tạo Leader mới.
- Đăng nhập bằng `username` hoặc `email` + `password`. Trả về JWT Access Token (hạn 24h) + Refresh Token (hạn 7 ngày).

---

## 4. DỰ ÁN (PROJECT) & SECTION HIERARCHY

- Leader tạo Project (Name, Description, Status: `Active`/`Archived`).
- Leader gán Tester vào Project (`project_members`). Tester chỉ thấy các Project mình được gán. Leader thấy tất cả.
- Quản lý Section/Subsection theo cây phân cấp trong từng Project.
- **Quyền Section**: Cả Leader và Tester (được gán) đều có quyền Tạo / Sửa Section & Subsection. **Xóa Section là quyền dành riêng cho Leader** (không cho phép xóa nếu Section có chứa Subsection con hoặc Test Case — trả về HTTP 409 Conflict).

---

## 5. DÒNG VÒNG ĐỜI TEST CASE & REVIEW WORKFLOW

### 5.1 Trạng thái Test Case
1. **Draft**: Mới tạo (bởi Leader/Tester) hoặc import từ Excel. Owner Tester hoặc Leader có thể sửa/xóa.
2. **Review**: Tester nhấn "Submit for Review". Khóa sửa/xóa đối với Owner Tester. Leader có thể Approve hoặc Reject.
3. **Ready**: Đã duyệt bởi Leader.
   - Nếu **Tester** sửa case ở `Ready` → Tự động chuyển về `Draft`.
   - Nếu **Leader** sửa case ở `Ready` → Giữ nguyên `Ready`.

### 5.2 Mã hiệu Test Case (Code)
Format `TC-%04d` (VD `TC-0001`), tự động sinh duy nhất toàn hệ thống từ primary key `id` sau khi insert.

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
| `test_runs` | id, project_id, milestone_id (nullable), name, status (Open/Closed), created_by, created_at, closed_at |
| `test_run_cases` | id, run_id, case_id, title, precondition, steps, expected_result, test_data (snapshot), assigned_to, result_status, executed_by, executed_at, comment, defect_ref, is_reviewed, reviewed_by, reviewed_at, review_comment |
| `execution_history` | id, run_case_id, result_status, comment, executed_by, executed_at |
| `attachments` | id, entity_type, entity_id, file_path, uploaded_by, uploaded_at |
| `api_tokens` | id, created_by, token_hash, revoked_at, created_at, last_used_at |
| `audit_logs` | id, user_id, action, entity_type, entity_id, detail_json, created_at |
| `excel_import_sessions` | id, import_session_id, project_id, created_by, parsed_payload_json, error_lines_json, expires_at, created_at |

---

## 8. ĐẶC TẢ IMPORT/EXPORT EXCEL

### 8.1 Cấu trúc Template Import / Export (Sheet-per-Root-Section)
Mỗi worksheet (sheet) trong file `.xlsx` đại diện cho **1 Section gốc (root Section)** trong Project. Tên sheet = Tên root Section (tối đa 31 ký tự).

| Cột | Tên Cột | Bắt buộc | Kiểu dữ liệu | Ghi chú |
|---|---|:---:|---|---|
| A | Subsection Path | ❌ | Text | Đường dẫn cây phân cấp bằng `>` (VD: `Parent > Child > Grandchild`). Rỗng = thuộc trực tiếp root Section |
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

---

## 9. THIẾT KẾ API CATALOG

Tra cứu danh sách endpoint chính trong `AI_CONTEXT.md` section 5 và `docs/architecture/api-contracts.md`.
