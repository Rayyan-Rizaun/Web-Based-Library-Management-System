package com.lms.common.domain;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Aggregate root {@link AuditLog}. "Track who performed each action" (security requirements).
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** History of one record, e.g. ("Fine", "12"). */
    List<AuditLog> findByEntityNameAndEntityIdOrderByOccurredAtDesc(String entityName, String entityId);

    Page<AuditLog> findByUserUserIdOrderByOccurredAtDesc(Integer userId, Pageable pageable);

    Page<AuditLog> findByOccurredAtBetweenOrderByOccurredAtDesc(LocalDateTime from, LocalDateTime to, Pageable pageable);

    /** System Logs screen: one search box against the acting user's name, the action and the entity, plus a date range. */
    @Query(value = """
            SELECT a FROM AuditLog a
            LEFT JOIN a.user u
            WHERE (:likeQuery IS NULL
                   OR LOWER(u.firstName) LIKE :likeQuery
                   OR LOWER(u.lastName) LIKE :likeQuery
                   OR LOWER(a.actionName) LIKE :likeQuery
                   OR LOWER(a.entityName) LIKE :likeQuery)
              AND (:from IS NULL OR a.occurredAt >= :from)
              AND (:to IS NULL OR a.occurredAt < :to)
            """,
            countQuery = """
            SELECT COUNT(a) FROM AuditLog a
            LEFT JOIN a.user u
            WHERE (:likeQuery IS NULL
                   OR LOWER(u.firstName) LIKE :likeQuery
                   OR LOWER(u.lastName) LIKE :likeQuery
                   OR LOWER(a.actionName) LIKE :likeQuery
                   OR LOWER(a.entityName) LIKE :likeQuery)
              AND (:from IS NULL OR a.occurredAt >= :from)
              AND (:to IS NULL OR a.occurredAt < :to)
            """)
    Page<AuditLog> search(@Param("likeQuery") String likeQuery, @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to, Pageable pageable);
}
