package com.testhub.testflowlite.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private Long id;
    private Long userId;
    private String action;
    private String entityType;
    private Long entityId;
    private String detailJson;
    private LocalDateTime createdAt;
}
