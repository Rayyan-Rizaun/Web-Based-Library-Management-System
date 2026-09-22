package com.lms.common.domain;

/**
 * {@code DatabaseBackupLog.BackupType} — CK_DatabaseBackupLog_BackupType.
 *
 * <p>Constant names are the stored values, so the column is mapped with
 * {@code @Enumerated(EnumType.STRING)}.
 */
public enum BackupType {
    Full, Differential, Log
}
