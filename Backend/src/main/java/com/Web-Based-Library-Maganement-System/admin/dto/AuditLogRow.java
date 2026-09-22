package com.lms.admin.dto;

import java.time.LocalDateTime;

public record AuditLogRow(Long auditId, String userName, String actionName, String entityName, String entityId,
        LocalDateTime occurredAt) {
}
