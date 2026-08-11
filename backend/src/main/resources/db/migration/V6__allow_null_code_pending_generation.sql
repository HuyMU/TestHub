-- V6__allow_null_code_pending_generation.sql: Allow NULL for test_cases.code during 2-phase code generation
ALTER TABLE test_cases MODIFY COLUMN code VARCHAR(20) NULL;
