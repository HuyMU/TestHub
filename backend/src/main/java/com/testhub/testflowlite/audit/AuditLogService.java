package com.testhub.testflowlite.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final JdbcTemplate jdbcTemplate;

    public void logAction(Long userId, String action, String entityType, Long entityId, String detailJson) {
        try {
            String sql = "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, detail_json, created_at) VALUES (?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, userId, action, entityType, entityId, detailJson, LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to write audit log: action={}, entityType={}, entityId={}", action, entityType, entityId, e);
        }
    }
}
