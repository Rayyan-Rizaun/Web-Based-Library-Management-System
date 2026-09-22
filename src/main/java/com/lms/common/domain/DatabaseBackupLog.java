package com.lms.common.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Table {@code DatabaseBackupLog} — entity DATABASE_BACKUP_LOG (1:N INITIATED BY). */
@Entity
@Table(name = "DatabaseBackupLog")
@Getter
@Setter
@NoArgsConstructor
public class DatabaseBackupLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BackupLogID")
    @Setter(AccessLevel.NONE)
    private Integer backupLogId;

    @Enumerated(EnumType.STRING)
    @Column(name = "BackupType", nullable = false, length = 15)
    private BackupType backupType;

    @Column(name = "BackupPath", nullable = false, length = 400)
    private String backupPath;

    @Column(name = "StartedAt", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    /** NULL while Running; required once Succeeded or Failed. */
    @Column(name = "CompletedAt")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 10)
    private BackupStatus status = BackupStatus.Running;

    /** NULL for the scheduled daily backup. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "InitiatedByUserID")
    private AppUser initiatedBy;

    /** Required when Failed. */
    @Column(name = "ErrorMessage", length = 2000)
    private String errorMessage;

    @PrePersist
    void onCreate() {
        if (startedAt == null) {
            startedAt = DbTime.now();
        }
    }
}
