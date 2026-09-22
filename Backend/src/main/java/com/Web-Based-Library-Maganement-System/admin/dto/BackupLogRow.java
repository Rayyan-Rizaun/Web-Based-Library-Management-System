package com.lms.admin.dto;

import java.time.LocalDateTime;

public record BackupLogRow(Integer backupLogId, String backupType, String status, LocalDateTime startedAt,
        LocalDateTime completedAt, String duration, String errorMessage) {
}
