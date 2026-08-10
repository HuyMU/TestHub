ALTER TABLE execution_history ADD COLUMN duration_ms BIGINT NULL AFTER result_status;
