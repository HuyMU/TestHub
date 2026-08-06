package com.testhub.testflowlite.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<AuditLogDto> auditLogRowMapper = (rs, rowNum) -> new AuditLogDto(
            rs.getLong("id"),
            rs.getObject("user_id") != null ? rs.getLong("user_id") : null,
            rs.getString("action"),
            rs.getString("entity_type"),
            rs.getObject("entity_id") != null ? rs.getLong("entity_id") : null,
            rs.getString("detail_json"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null
    );

    public void logAction(Long userId, String action, String entityType, Long entityId, String detailJson) {
        try {
            String sql = "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, detail_json, created_at) VALUES (?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, userId, action, entityType, entityId, detailJson, LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to write audit log: action={}, entityType={}, entityId={}", action, entityType, entityId, e);
        }
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getAuditLogs(String entityType, Long userId) {
        StringBuilder sql = new StringBuilder("SELECT id, user_id, action, entity_type, entity_id, detail_json, created_at FROM audit_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (entityType != null && !entityType.isBlank()) {
            sql.append(" AND entity_type = ?");
            params.add(entityType);
        }
        if (userId != null) {
            sql.append(" AND user_id = ?");
            params.add(userId);
        }
        sql.append(" ORDER BY id DESC LIMIT 200");

        return jdbcTemplate.query(sql.toString(), auditLogRowMapper, params.toArray());
    }
}
