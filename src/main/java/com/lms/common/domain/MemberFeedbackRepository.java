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
 * Aggregate root {@link MemberFeedback}, including its {@link FeedbackHistory}.
 * UC-10 (submit, view, edit, respond, filter by category, date or status).
 */
public interface MemberFeedbackRepository extends JpaRepository<MemberFeedback, Integer> {

    /** "My Feedback". */
    List<MemberFeedback> findByMemberMemberIdOrderBySubmittedAtDesc(Integer memberId);

    /** Look up by the reference number given to the member. */
    Optional<MemberFeedback> findByFeedbackReference(String feedbackReference);

    boolean existsByFeedbackReference(String feedbackReference);

    Page<MemberFeedback> findByStatusOrderBySubmittedAtAsc(FeedbackStatus status, Pageable pageable);

    Page<MemberFeedback> findByCategoryFeedbackCategoryIdAndStatusOrderBySubmittedAtAsc(Integer feedbackCategoryId,
                                                                                         FeedbackStatus status,
                                                                                         Pageable pageable);

    List<MemberFeedback> findByPriorityAndStatusOrderBySubmittedAtAsc(FeedbackPriority priority, FeedbackStatus status);

    List<MemberFeedback> findBySubmittedAtBetweenOrderBySubmittedAtDesc(LocalDateTime from, LocalDateTime to);

    /**
     * The staff "Feedback" list screen: one search box matched against the
     * reference, subject, and the submitting member's first/last name all
     * at once, plus optional status/category/priority filters — all applied
     * together, with paging and sorting. Mirrors {@code
     * BookRepository.search}'s exact "optional {@code :param IS NULL OR
     * ...}" idiom for the same reason: none of the single-field finder
     * methods above can express "search AND filter AND sort" as one
     * request. {@code com.lms.feedback.FeedbackService} builds every
     * argument (a lower-cased {@code %pattern%} for {@code likeQuery}, or
     * {@code null} to skip that filter) so this method stays a plain
     * query, not a place business rules live (CLAUDE.md rule 5).
     */
    @Query(value = """
            SELECT f FROM MemberFeedback f
            JOIN f.member m
            JOIN m.user u
            WHERE (:likeQuery IS NULL
                   OR LOWER(f.feedbackReference) LIKE :likeQuery
                   OR LOWER(f.subject) LIKE :likeQuery
                   OR LOWER(u.firstName) LIKE :likeQuery
                   OR LOWER(u.lastName) LIKE :likeQuery)
              AND (:status IS NULL OR f.status = :status)
              AND (:categoryId IS NULL OR f.category.feedbackCategoryId = :categoryId)
              AND (:priority IS NULL OR f.priority = :priority)
            """,
            countQuery = """
            SELECT COUNT(f) FROM MemberFeedback f
            JOIN f.member m
            JOIN m.user u
            WHERE (:likeQuery IS NULL
                   OR LOWER(f.feedbackReference) LIKE :likeQuery
                   OR LOWER(f.subject) LIKE :likeQuery
                   OR LOWER(u.firstName) LIKE :likeQuery
                   OR LOWER(u.lastName) LIKE :likeQuery)
              AND (:status IS NULL OR f.status = :status)
              AND (:categoryId IS NULL OR f.category.feedbackCategoryId = :categoryId)
              AND (:priority IS NULL OR f.priority = :priority)
            """)
    Page<MemberFeedback> search(@Param("likeQuery") String likeQuery, @Param("status") FeedbackStatus status,
                                 @Param("categoryId") Integer categoryId, @Param("priority") FeedbackPriority priority,
                                 Pageable pageable);

    @Query(value = """
            SELECT fc.CategoryName AS CategoryName, mf.Status AS Status, COUNT(*) AS Cnt
            FROM MemberFeedback mf
            JOIN FeedbackCategory fc ON fc.FeedbackCategoryID = mf.FeedbackCategoryID
            WHERE mf.SubmittedAt BETWEEN :from AND :to
              AND (:categoryId IS NULL OR fc.FeedbackCategoryID = :categoryId)
            GROUP BY fc.CategoryName, mf.Status
            ORDER BY fc.CategoryName ASC, mf.Status ASC
            """, nativeQuery = true)
    List<Object[]> feedbackByCategoryAndStatus(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
            @Param("categoryId") Integer categoryId);

    @Query(value = """
            SELECT COUNT(*) AS TotalSubmissions,
                   SUM(CASE WHEN mf.Status IN (N'Resolved', N'Closed') THEN 1 ELSE 0 END) AS ResolvedOrClosed,
                   AVG(CASE WHEN mf.Status IN (N'Resolved', N'Closed')
                            THEN CAST(DATEDIFF(HOUR, mf.SubmittedAt, fh.FirstResolvedAt) AS FLOAT) / 24.0 END) AS AvgResolutionDays
            FROM MemberFeedback mf
            OUTER APPLY (
                SELECT MIN(ChangedAt) AS FirstResolvedAt FROM FeedbackHistory h
                WHERE h.FeedbackID = mf.FeedbackID AND h.NewStatus IN (N'Resolved', N'Closed')
            ) fh
            WHERE mf.SubmittedAt BETWEEN :from AND :to
              AND (:categoryId IS NULL OR mf.FeedbackCategoryID = :categoryId)
            """, nativeQuery = true)
    List<Object[]> feedbackSummary(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
            @Param("categoryId") Integer categoryId);
}
