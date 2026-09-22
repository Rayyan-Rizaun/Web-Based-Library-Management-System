package com.lms.common.domain;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Aggregate root {@link BookReview}, including its {@link ReviewFlag}s and moderation history.
 * UC-09 (rate, review, edit, remove) and the moderation queue (business-rules §6).
 */
public interface BookReviewRepository extends JpaRepository<BookReview, Integer> {

    /** A book's reviews shown on its catalogue page — pass Approved. */
    List<BookReview> findByBookBookIdAndStatusOrderBySubmittedAtDesc(Integer bookId, ReviewStatus status);

    /** "Total number of ratings" for a book. */
    long countByBookBookIdAndStatus(Integer bookId, ReviewStatus status);

    /** UC-09: edit the existing review instead of creating a duplicate. */
    Optional<BookReview> findByBookBookIdAndMemberMemberId(Integer bookId, Integer memberId);

    boolean existsByBookBookIdAndMemberMemberId(Integer bookId, Integer memberId);

    /** A member's review history. */
    List<BookReview> findByMemberMemberIdOrderBySubmittedAtDesc(Integer memberId);

    /** Moderation queue — pass Pending. */
    Page<BookReview> findByStatusOrderBySubmittedAtAsc(ReviewStatus status, Pageable pageable);

    /** Admin dashboard stat tile: reviews awaiting a moderation decision — Pending (new) or Hidden (flagged, awaiting re-decision). */
    long countByStatusIn(Collection<ReviewStatus> statuses);

    /** Flagged-review queue — pass {@code FlagStatus.Open}. */
    List<BookReview> findDistinctByFlagsStatusOrderBySubmittedAtAsc(FlagStatus status);

    /** Has this member already flagged this review? */
    boolean existsByReviewIdAndFlagsReportedByMemberId(Integer reviewId, Integer memberId);

    /**
     * Staff "Review Moderation" queue: search by book title or member name,
     * optionally filtered to one status, ordered Pending first, then Hidden,
     * then Approved and Rejected (business-rules §6) via a native CASE — no
     * JPQL equivalent orders by an enum-mapped column this way.
     */
    @Query(value = """
            SELECT r.* FROM BookReview r
            JOIN Book b ON b.BookID = r.BookID
            JOIN Member m ON m.MemberID = r.MemberID
            JOIN AppUser u ON u.UserID = m.UserID
            WHERE (:status IS NULL OR r.Status = :status)
              AND (:likeQuery IS NULL OR LOWER(b.Title) LIKE :likeQuery
                   OR LOWER(u.FirstName) LIKE :likeQuery OR LOWER(u.LastName) LIKE :likeQuery)
            ORDER BY CASE r.Status
                WHEN N'Pending' THEN 0
                WHEN N'Hidden' THEN 1
                WHEN N'Approved' THEN 2
                WHEN N'Rejected' THEN 3
                ELSE 4
            END, r.SubmittedAt ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM BookReview r
            JOIN Book b ON b.BookID = r.BookID
            JOIN Member m ON m.MemberID = r.MemberID
            JOIN AppUser u ON u.UserID = m.UserID
            WHERE (:status IS NULL OR r.Status = :status)
              AND (:likeQuery IS NULL OR LOWER(b.Title) LIKE :likeQuery
                   OR LOWER(u.FirstName) LIKE :likeQuery OR LOWER(u.LastName) LIKE :likeQuery)
            """,
            nativeQuery = true)
    Page<BookReview> searchQueue(@Param("status") String status, @Param("likeQuery") String likeQuery, Pageable pageable);

    @Query(value = """
            SELECT TOP 20 b.Title AS BookTitle,
                   COUNT(*) AS Submitted,
                   SUM(CASE WHEN r.Status = N'Approved' THEN 1 ELSE 0 END) AS Approved,
                   SUM(CASE WHEN r.Status = N'Rejected' THEN 1 ELSE 0 END) AS Rejected,
                   AVG(CASE WHEN r.Status = N'Approved' THEN CAST(r.Rating AS FLOAT) END) AS AvgRating
            FROM BookReview r
            JOIN Book b ON b.BookID = r.BookID
            WHERE r.SubmittedAt BETWEEN :from AND :to
            GROUP BY b.BookID, b.Title
            ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<Object[]> reviewsByBook(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT COUNT(*) AS Submitted,
                   SUM(CASE WHEN r.Status = N'Approved' THEN 1 ELSE 0 END) AS Approved,
                   SUM(CASE WHEN r.Status = N'Rejected' THEN 1 ELSE 0 END) AS Rejected,
                   AVG(CASE WHEN r.Status = N'Approved' THEN CAST(r.Rating AS FLOAT) END) AS AvgRating
            FROM BookReview r
            WHERE r.SubmittedAt BETWEEN :from AND :to
            """, nativeQuery = true)
    List<Object[]> reviewsSummary(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
