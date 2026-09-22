package com.lms.common.domain;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Aggregate root {@link FailedLoginAttempt}. Account lockout and the security review.
 */
public interface FailedLoginAttemptRepository extends JpaRepository<FailedLoginAttempt, Long> {

    /** Recent failures for one email, for lockout. */
    long countByEmailTriedIgnoreCaseAndAttemptedAtAfter(String emailTried, LocalDateTime since);

    Page<FailedLoginAttempt> findByAttemptedAtAfterOrderByAttemptedAtDesc(LocalDateTime since, Pageable pageable);

    /** System Logs screen: the email tried, and a date range. */
    @Query(value = """
            SELECT f FROM FailedLoginAttempt f
            WHERE (:likeQuery IS NULL OR LOWER(f.emailTried) LIKE :likeQuery)
              AND (:from IS NULL OR f.attemptedAt >= :from)
              AND (:to IS NULL OR f.attemptedAt < :to)
            """,
            countQuery = """
            SELECT COUNT(f) FROM FailedLoginAttempt f
            WHERE (:likeQuery IS NULL OR LOWER(f.emailTried) LIKE :likeQuery)
              AND (:from IS NULL OR f.attemptedAt >= :from)
              AND (:to IS NULL OR f.attemptedAt < :to)
            """)
    Page<FailedLoginAttempt> search(@Param("likeQuery") String likeQuery, @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to, Pageable pageable);
}
