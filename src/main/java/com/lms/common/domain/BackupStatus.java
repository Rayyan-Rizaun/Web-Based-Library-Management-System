package com.lms.common.domain;

/**
 * {@code DatabaseBackupLog.Status} — CK_DatabaseBackupLog_Status.
 *
 * <p>Constant names are the stored values, so the column is mapped with
 * {@code @Enumerated(EnumType.STRING)}.
 */
public enum BackupStatus {
    Running, Succeeded, Failed
}
