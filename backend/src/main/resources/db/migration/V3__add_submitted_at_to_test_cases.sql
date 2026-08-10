-- V3__add_submitted_at_to_test_cases.sql
ALTER TABLE test_cases ADD COLUMN submitted_at DATETIME NULL AFTER reviewed_at;
