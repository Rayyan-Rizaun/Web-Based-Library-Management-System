package com.lms.common.domain;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Aggregate root {@link Reservation}. UC-04 (reserve, queue, cancel, expire) and the UC-03
 * renewal block. The queue is per title and FIFO (business-rules §3).
 */
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    /** UC-04: "already reserved the same book" — pass Waiting and Ready. */
    boolean existsByBookBookIdAndMemberMemberIdAndStatusIn(Integer bookId, Integer memberId,
                                                           Collection<ReservationStatus> statuses);

    /** UC-03 renewal block: is ANOTHER member waiting for this title? */
    boolean existsByBookBookIdAndMemberMemberIdNotAndStatusIn(Integer bookId, Integer memberId,
                                                              Collection<ReservationStatus> statuses);

    /** The waiting list for a title, first come first served. */
    List<Reservation> findByBookBookIdAndStatusOrderByRequestedAtAsc(Integer bookId, ReservationStatus status);

    /** Who gets the next returned copy. */
    Optional<Reservation> findFirstByBookBookIdAndStatusOrderByRequestedAtAsc(Integer bookId, ReservationStatus status);

    /**
     * Queue position without a stored column (R12): the number of Waiting
     * reservations for the book requested before this one, plus one.
     */
    long countByBookBookIdAndStatusAndRequestedAtBefore(Integer bookId, ReservationStatus status,
                                                        LocalDateTime requestedAt);

    List<Reservation> findByMemberMemberIdOrderByRequestedAtDesc(Integer memberId);

    /** Holds not collected in time: Ready reservations whose ExpiresAt has passed. */
    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime now);

    /** UC-04 staff "Reservation Queue" screen: every still-open reservation (Waiting or Ready), oldest first. */
    List<Reservation> findByStatusInOrderByRequestedAtAsc(Collection<ReservationStatus> statuses);

    /** Dashboard stat tiles: how many reservations are currently in this state, system-wide. */
    long countByStatus(ReservationStatus status);

    /** Dashboard list: the soonest-ready holds still waiting for collection. */
    List<Reservation> findTop5ByStatusOrderByReadyAtAsc(ReservationStatus status);

    /** Member dashboard stat tile: this member's own active reservations (Waiting or Ready). */
    long countByMemberMemberIdAndStatusIn(Integer memberId, Collection<ReservationStatus> statuses);

    /** UC-08 reservations report. */
    List<Reservation> findByRequestedAtBetweenOrderByRequestedAtAsc(LocalDateTime from, LocalDateTime to);

    @Query(value = """
            SELECT TOP 20 b.Title AS BookTitle,
                   COUNT(*) AS Requested,
                   SUM(CASE WHEN r.Status = N'Fulfilled' THEN 1 ELSE 0 END) AS Fulfilled,
                   SUM(CASE WHEN r.Status = N'Expired' THEN 1 ELSE 0 END) AS Expired,
                   (SELECT COUNT(*) FROM Reservation r2 WHERE r2.BookID = r.BookID AND r2.Status = N'Waiting') AS CurrentQueueLength
            FROM Reservation r
            JOIN Book b ON b.BookID = r.BookID
            WHERE r.RequestedAt BETWEEN :from AND :to
            GROUP BY r.BookID, b.Title
            ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<Object[]> reservationsByTitle(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT COUNT(*) AS TotalReservations,
                   SUM(CASE WHEN r.Status = N'Fulfilled' THEN 1 ELSE 0 END) AS FulfilledCount,
                   SUM(CASE WHEN r.Status = N'Expired' THEN 1 ELSE 0 END) AS ExpiredCount
            FROM Reservation r
            WHERE r.RequestedAt BETWEEN :from AND :to
            """, nativeQuery = true)
    List<Object[]> reservationsSummary(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
