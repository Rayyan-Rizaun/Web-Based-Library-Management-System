package com.lms.admin;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.admin.dto.AuditLogRow;
import com.lms.admin.dto.BackupLogRow;
import com.lms.admin.dto.FailedLoginRow;
import com.lms.common.domain.AuditLog;
import com.lms.common.domain.AuditLogRepository;
import com.lms.common.domain.BackupStatus;
import com.lms.common.domain.BackupType;
import com.lms.common.domain.DatabaseBackupLog;
import com.lms.common.domain.DatabaseBackupLogRepository;
import com.lms.common.domain.FailedLoginAttempt;
import com.lms.common.domain.FailedLoginAttemptRepository;

@Service
@Transactional(readOnly = true)
public class SystemLogService {

    private static final String ADMIN_ROLE = "hasAuthority('Library Administrator')";

    static final int PAGE_SIZE = 20;

    private final AuditLogRepository auditLogs;
    private final FailedLoginAttemptRepository failedLogins;
    private final DatabaseBackupLogRepository backupLogs;

    public SystemLogService(AuditLogRepository auditLogs, FailedLoginAttemptRepository failedLogins,
            DatabaseBackupLogRepository backupLogs) {
        this.auditLogs = auditLogs;
        this.failedLogins = failedLogins;
        this.backupLogs = backupLogs;
    }

    @PreAuthorize(ADMIN_ROLE)
    public Page<AuditLogRow> auditLog(String q, LocalDateTime from, LocalDateTime to, int page) {
        String likeQuery = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "occurredAt"));
        return auditLogs.search(likeQuery, from, to, pageable).map(this::toRow);
    }

    private AuditLogRow toRow(AuditLog a) {
        String userName = a.getUser() == null ? "System" : a.getUser().getFirstName() + " " + a.getUser().getLastName();
        return new AuditLogRow(a.getAuditId(), userName, a.getActionName(), a.getEntityName(), a.getEntityId(), a.getOccurredAt());
    }

    @PreAuthorize(ADMIN_ROLE)
    public Page<FailedLoginRow> failedLogins(String q, LocalDateTime from, LocalDateTime to, int page) {
        String likeQuery = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "attemptedAt"));
        return failedLogins.search(likeQuery, from, to, pageable).map(this::toRow);
    }

    private FailedLoginRow toRow(FailedLoginAttempt f) {
        return new FailedLoginRow(f.getAttemptId(), f.getEmailTried(), f.getFailureReason().name(), f.getIpAddress(), f.getAttemptedAt());
    }

    @PreAuthorize(ADMIN_ROLE)
    public Page<BackupLogRow> backupLogs(String status, String type, LocalDateTime from, LocalDateTime to, int page) {
        BackupStatus statusFilter = (status == null || status.isBlank()) ? null : BackupStatus.valueOf(status);
        BackupType typeFilter = (type == null || type.isBlank()) ? null : BackupType.valueOf(type);
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "startedAt"));
        return backupLogs.search(statusFilter, typeFilter, from, to, pageable).map(this::toRow);
    }

    private BackupLogRow toRow(DatabaseBackupLog b) {
        String duration = b.getCompletedAt() == null ? "—" : formatDuration(Duration.between(b.getStartedAt(), b.getCompletedAt()));
        return new BackupLogRow(b.getBackupLogId(), b.getBackupType().name(), b.getStatus().name(), b.getStartedAt(),
                b.getCompletedAt(), duration, b.getErrorMessage());
    }

    private static String formatDuration(Duration duration) {
        long totalSeconds = Math.max(0, duration.getSeconds());
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + "m " + seconds + "s";
    }
}
