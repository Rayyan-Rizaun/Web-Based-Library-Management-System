package com.lms.common.domain;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Aggregate root {@link BookIncident}. Lost and damaged copies, and the UC-08 lost/damaged report.
 */
public interface BookIncidentRepository extends JpaRepository<BookIncident, Integer> {

    List<BookIncident> findByCopyCopyIdOrderByReportedAtDesc(Integer copyId);

    List<BookIncident> findByStatusOrderByReportedAtAsc(IncidentStatus status);

    List<BookIncident> findByReportedAtBetweenOrderByReportedAtAsc(LocalDateTime from, LocalDateTime to);
}
