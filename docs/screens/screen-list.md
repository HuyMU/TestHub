# UI Screen Catalog — TestFlow Lite

Below is the complete catalog of 15 screens specified in Section 10 of `DacTa-TestFlowLite-SRS.md`.

| # | Screen Name | Access Role | Description |
|---|---|:---:|---|
| 1 | **Login** | Public | User authentication form (Username/Email + Password) |
| 2 | **Dashboard (by Project)** | Leader / Tester | Overview metrics: Pass/Fail/Blocked rates, Review Queue counter, Milestone progress |
| 3 | **Project List** | Leader | Complete project catalog with search and Active/Archived toggles |
| 4 | **Project Details** | Leader / Tester | Tabbed project interface: Sections/Cases, Runs, Milestones, Members |
| 5 | **Section Hierarchy & Test Cases** | Leader / Tester | Tree navigation for Sections/Subsections + filtered Test Case table |
| 6 | **Create / Edit Test Case Form** | Leader / Tester | 10-field editor with "Submit for Review" button |
| 7 | **Review Queue** | Leader | Batch approval screen for cases pending review (`Review` -> `Ready` / `Draft`) |
| 8 | **Excel Import Wizard** | Leader / Tester | 2-step import dialog (Step 1: Validate/Preview errors, Step 2: Confirm) |
| 9 | **Milestone Management** | Leader | List and creation modal for Milestones (Name, Due Date, Status) |
| 10 | **Create Test Run** | Leader | Run creation wizard (Select `Ready` cases, optional Milestone assignment) |
| 11 | **Test Execution Interface** | Tester / Leader | Execution workbench for recording execution result, comments, defect links |
| 12 | **Review Execution Results** | Leader | Leader review of execution results (`Reviewed` or `Request Retest`) |
| 13 | **Test Run Detailed Report** | Leader / Tester | Summary breakdown of a Test Run + Excel export button |
| 14 | **Tester User Management** | Leader | Management table for creating, updating, and deactivating Tester accounts |
| 15 | **API Token Management** | Leader | Generation and revocation of API Tokens for Automation API ingestion |
