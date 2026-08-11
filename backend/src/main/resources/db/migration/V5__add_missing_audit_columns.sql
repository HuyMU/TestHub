-- V5__add_missing_audit_columns.sql: Add missing created_at and updated_at columns to entities extending BaseEntity

ALTER TABLE users ADD COLUMN updated_at DATETIME NULL;

ALTER TABLE projects ADD COLUMN updated_at DATETIME NULL;

ALTER TABLE sections ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE sections ADD COLUMN updated_at DATETIME NULL;
