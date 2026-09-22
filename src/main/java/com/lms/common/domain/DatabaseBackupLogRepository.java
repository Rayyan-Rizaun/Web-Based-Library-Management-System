package com.lms.common.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Aggregate root {@link DatabaseBackupLog}. Backup history (security requirements).
 */
public interface DatabaseBackupLogRepository extends JpaRepository<DatabaseBackupLog, Integer> {

    List<DatabaseBackupLog> findTop10ByOrderByStartedAtDesc();

    Optional<DatabaseBackupLog> findFirstByStatusOrderByStartedAtDesc(BackupStatus status);

    /** System Logs screen: optional status/type filter, plus a date range. */
    @Query(value = """
            SELECT b FROM DatabaseBackupLog b
            WHERE (:statusFilter IS NULL OR b.status = :statusFilter)
              AND (:typeFilter IS NULL OR b.backupType = :typeFilter)
              AND (:from IS NULL OR b.startedAt >= :from)
              AND (:to IS NULL OR b.startedAt < :to)
            """,
            countQuery = """
            SELECT COUNT(b) FROM DatabaseBackupLog b
            WHERE (:statusFilter IS NULL OR b.status = :statusFilter)
              AND (:typeFilter IS NULL OR b.backupType = :typeFilter)
              AND (:from IS NULL OR b.startedAt >= :from)
              AND (:to IS NULL OR b.startedAt < :to)
            """)
    Page<DatabaseBackupLog> search(@Param("statusFilter") BackupStatus statusFilter, @Param("typeFilter") BackupType typeFilter,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}
