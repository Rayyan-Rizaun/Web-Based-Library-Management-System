package com.lms.common.domain;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Aggregate root {@link ReportAudit}. UC-08: the record of generated reports.
 */
public interface ReportAuditRepository extends JpaRepository<ReportAudit, Integer> {

    List<ReportAudit> findByRequestedByUserIdOrderByGeneratedAtDesc(Integer userId);

    List<ReportAudit> findByReportTypeOrderByGeneratedAtDesc(ReportType reportType);

    @Query(value = """
            SELECT ra FROM ReportAudit ra
            JOIN ra.requestedBy u
            WHERE (:likeQuery IS NULL OR LOWER(u.firstName) LIKE :likeQuery OR LOWER(u.lastName) LIKE :likeQuery)
              AND (:reportType IS NULL OR ra.reportType = :reportType)
              AND (:from IS NULL OR ra.generatedAt >= :from)
              AND (:to IS NULL OR ra.generatedAt <= :to)
            """,
            countQuery = """
            SELECT COUNT(ra) FROM ReportAudit ra
            JOIN ra.requestedBy u
            WHERE (:likeQuery IS NULL OR LOWER(u.firstName) LIKE :likeQuery OR LOWER(u.lastName) LIKE :likeQuery)
              AND (:reportType IS NULL OR ra.reportType = :reportType)
              AND (:from IS NULL OR ra.generatedAt >= :from)
              AND (:to IS NULL OR ra.generatedAt <= :to)
            """)
    Page<ReportAudit> search(@Param("likeQuery") String likeQuery, @Param("reportType") ReportType reportType,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}
