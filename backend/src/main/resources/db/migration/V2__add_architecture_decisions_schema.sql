-- V2__add_architecture_decisions_schema.sql: Architecture decisions updates

-- 1. Create excel_import_sessions table
CREATE TABLE IF NOT EXISTS excel_import_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    import_session_id VARCHAR(50) NOT NULL UNIQUE,
    project_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    parsed_payload_json LONGTEXT,
    error_lines_json TEXT,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eis_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_eis_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

-- 2. Update api_tokens table with token_hash rename and revoked_at column
ALTER TABLE api_tokens CHANGE COLUMN token token_hash VARCHAR(255) NOT NULL;
ALTER TABLE api_tokens ADD COLUMN revoked_at DATETIME AFTER last_used_at;

-- 3. Update test_run_cases table with snapshot columns
ALTER TABLE test_run_cases ADD COLUMN title VARCHAR(255) AFTER case_id;
ALTER TABLE test_run_cases ADD COLUMN precondition TEXT AFTER title;
ALTER TABLE test_run_cases ADD COLUMN steps TEXT AFTER precondition;
ALTER TABLE test_run_cases ADD COLUMN expected_result TEXT AFTER steps;
ALTER TABLE test_run_cases ADD COLUMN test_data TEXT AFTER expected_result;
