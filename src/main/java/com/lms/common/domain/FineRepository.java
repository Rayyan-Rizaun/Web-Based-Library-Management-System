package com.lms.common.domain;

import java.math.BigDecimal;
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
 * Aggregate root {@link Fine}, including its {@link FineAppeal}s and {@link FinePayment}s.
 * UC-05 (manage fines), UC-06 (appeals), UC-07 (payments and receipts), the UC-03
 * borrowing block, and UC-08 fine reports.
 *
 * <p>The outstanding balance is not a column (R18); compute it from the fine's
 * approved appeals and completed payments.
 */
public interface FineRepository extends JpaRepository<Fine, Integer> {

    /** All of a member's fines, newest first. */
    List<Fine> findByMemberMemberIdOrderByAssessedAtDesc(Integer memberId);

    /** A member's outstanding fines — pass Pending, Under Appeal and Partially Paid. */
    List<Fine> findByMemberMemberIdAndStatusInOrderByAssessedAtAsc(Integer memberId, Collection<FineStatus> statuses);

    /** UC-05 Finance Officer's outstanding-fines list. */
    Page<Fine> findByStatusInOrderByAssessedAtAsc(Collection<FineStatus> statuses, Pageable pageable);

    /** Unpaged — the Outstanding Fines stat tile totals a balance across every matching fine, not just one page. */
    List<Fine> findByStatusIn(Collection<FineStatus> statuses);

    /** UC-06 staff decision: the fine that owns this appeal, reached the same way {@code FinePayment}'s owner already is — there is no separate FineAppealRepository (part of the Fine aggregate). */
    Optional<Fine> findByAppealsAppealId(Integer appealId);

    /** UC-03 borrowing block: an unpaid fine of this type (business-rules §1). */
    boolean existsByMemberMemberIdAndFineTypeAndStatusIn(Integer memberId, FineType fineType,
                                                         Collection<FineStatus> statuses);

    /** The overdue fine raised when a loan is returned late (at most one per loan). */
    Optional<Fine> findByLoanLoanIdAndFineType(Integer loanId, FineType fineType);

    /** The fine raised for a lost or damaged copy (at most one per incident). */
    Optional<Fine> findByIncidentIncidentId(Integer incidentId);

    /** UC-06 review queue: fines with an appeal in the given status, e.g. Pending. */
    List<Fine> findDistinctByAppealsStatusOrderByAssessedAtAsc(AppealStatus status);

    /** Finance dashboard stat tile: how many distinct fines have an appeal in the given status, e.g. Pending. */
    long countDistinctByAppealsStatus(AppealStatus status);

    /** UC-07 "reprint previous receipts". */
    Optional<Fine> findByPaymentsReceiptNumber(String receiptNumber);

    boolean existsByPaymentsReceiptNumber(String receiptNumber);

    /** UC-08 fines and waived-fines reports. */
    List<Fine> findByStatusAndAssessedAtBetweenOrderByAssessedAtAsc(FineStatus status, LocalDateTime from,
                                                                   LocalDateTime to);

    List<Fine> findByAssessedAtBetweenOrderByAssessedAtAsc(LocalDateTime from, LocalDateTime to);

    /** UC-08 fine collection report: fines with a payment made in the period. */
    List<Fine> findDistinctByPaymentsPaymentStatusAndPaymentsPaidAtBetween(PaymentStatus paymentStatus,
                                                                         LocalDateTime from, LocalDateTime to);

    /**
     * The staff "Outstanding Fines" screen: always narrowed to {@code
     * outstandingStatuses} (Pending, Under Appeal, Partially Paid — never
     * Fully Paid or Waived, whatever {@code statusFilter} says), then one
     * search box matched against the member's first/last name and
     * membership number at once, an optional narrower status filter, an
     * optional fine-type filter, all applied together with paging and
     * sorting. Mirrors {@code BookRepository.search}'s exact "optional
     * {@code :param IS NULL OR ...}" idiom. {@code com.lms.fine.FineService}
     * builds every argument, so this stays a plain query, not a place
     * business rules live (CLAUDE.md rule 5).
     */
    @Query(value = """
            SELECT f FROM Fine f
            JOIN f.member m
            JOIN m.user u
            WHERE f.status IN :outstandingStatuses
              AND (:statusFilter IS NULL OR f.status = :statusFilter)
              AND (:typeFilter IS NULL OR f.fineType = :typeFilter)
              AND (:likeQuery IS NULL
                   OR LOWER(u.firstName) LIKE :likeQuery
                   OR LOWER(u.lastName) LIKE :likeQuery
                   OR LOWER(m.membershipNo) LIKE :likeQuery)
            """,
            countQuery = """
            SELECT COUNT(f) FROM Fine f
            JOIN f.member m
            JOIN m.user u
            WHERE f.status IN :outstandingStatuses
              AND (:statusFilter IS NULL OR f.status = :statusFilter)
              AND (:typeFilter IS NULL OR f.fineType = :typeFilter)
              AND (:likeQuery IS NULL
                   OR LOWER(u.firstName) LIKE :likeQuery
                   OR LOWER(u.lastName) LIKE :likeQuery
                   OR LOWER(m.membershipNo) LIKE :likeQuery)
            """)
    Page<Fine> searchOutstanding(@Param("outstandingStatuses") Collection<FineStatus> outstandingStatuses,
                                  @Param("statusFilter") FineStatus statusFilter,
                                  @Param("typeFilter") FineType typeFilter,
                                  @Param("likeQuery") String likeQuery,
                                  Pageable pageable);

    @Query(value = """
            SELECT f.FineType AS FineType,
                   SUM(f.AmountAssessed) AS Assessed,
                   ISNULL(SUM(p.CollectedAmt), 0) AS Collected,
                   SUM(CASE WHEN f.Status = N'Waived' THEN f.AmountAssessed ELSE 0 END) AS Waived,
                   SUM(CASE WHEN f.Status NOT IN (N'Waived', N'Fully Paid')
                            THEN f.AmountAssessed - ISNULL(r.ReductionAmt, 0) - ISNULL(p.CollectedAmt, 0)
                            ELSE 0 END) AS Outstanding
            FROM Fine f
            OUTER APPLY (SELECT SUM(AmountPaid) AS CollectedAmt FROM FinePayment
                         WHERE FineID = f.FineID AND PaymentStatus = N'Completed') p
            OUTER APPLY (SELECT SUM(ApprovedReduction) AS ReductionAmt FROM FineAppeal
                         WHERE FineID = f.FineID AND Status = N'Approved') r
            WHERE f.AssessedAt BETWEEN :from AND :to
              AND (:fineType IS NULL OR f.FineType = :fineType)
            GROUP BY f.FineType
            """, nativeQuery = true)
    List<Object[]> finesByType(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
            @Param("fineType") String fineType);

    /** Admin/Finance dashboard stat tile: R18 balance, summed across every fine still owed, system-wide. */
    @Query(value = """
            SELECT ISNULL(SUM(
                CASE WHEN f.Status NOT IN (N'Waived', N'Fully Paid')
                     THEN f.AmountAssessed - ISNULL(r.ReductionAmt, 0) - ISNULL(p.CollectedAmt, 0)
                     ELSE 0 END
            ), 0)
            FROM Fine f
            OUTER APPLY (SELECT SUM(AmountPaid) AS CollectedAmt FROM FinePayment
                         WHERE FineID = f.FineID AND PaymentStatus = N'Completed') p
            OUTER APPLY (SELECT SUM(ApprovedReduction) AS ReductionAmt FROM FineAppeal
                         WHERE FineID = f.FineID AND Status = N'Approved') r
            """, nativeQuery = true)
    BigDecimal outstandingBalanceTotal();

    /** Member dashboard stat tile: the same R18 balance, narrowed to one member. */
    @Query(value = """
            SELECT ISNULL(SUM(
                CASE WHEN f.Status NOT IN (N'Waived', N'Fully Paid')
                     THEN f.AmountAssessed - ISNULL(r.ReductionAmt, 0) - ISNULL(p.CollectedAmt, 0)
                     ELSE 0 END
            ), 0)
            FROM Fine f
            OUTER APPLY (SELECT SUM(AmountPaid) AS CollectedAmt FROM FinePayment
                         WHERE FineID = f.FineID AND PaymentStatus = N'Completed') p
            OUTER APPLY (SELECT SUM(ApprovedReduction) AS ReductionAmt FROM FineAppeal
                         WHERE FineID = f.FineID AND Status = N'Approved') r
            WHERE f.MemberID = :memberId
            """, nativeQuery = true)
    BigDecimal outstandingBalanceForMember(@Param("memberId") Integer memberId);

    /** Finance dashboard stat tile: completed payments taken in a window (e.g. this calendar month). */
    @Query(value = """
            SELECT ISNULL(SUM(AmountPaid), 0) FROM FinePayment
            WHERE PaymentStatus = N'Completed' AND PaidAt BETWEEN :from AND :to
            """, nativeQuery = true)
    BigDecimal collectedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Finance dashboard stat tile: fines waived in a window. {@code Fine} has
     * no {@code WaivedAt} column (schema not touched for this task), so
     * "waived this month" is read off the {@code AuditLog} row {@code
     * FineService.waive}'s own {@code @AuditAction} already writes for every
     * waiver, joined back to the fine it waived.
     */
    @Query(value = """
            SELECT ISNULL(SUM(f.AmountAssessed), 0)
            FROM Fine f
            JOIN AuditLog a ON a.EntityName = N'Fine' AND a.ActionName = N'WAIVE' AND a.EntityID = CAST(f.FineID AS NVARCHAR(20))
            WHERE f.Status = N'Waived' AND a.OccurredAt BETWEEN :from AND :to
            """, nativeQuery = true)
    BigDecimal waivedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Admin/Finance dashboard list: the five largest outstanding balances, system-wide. */
    @Query(value = """
            SELECT TOP 5 f.FineID AS FineID, u.FirstName + N' ' + u.LastName AS MemberName, f.FineType AS FineType,
                   f.AmountAssessed - ISNULL(r.ReductionAmt, 0) - ISNULL(p.CollectedAmt, 0) AS Balance
            FROM Fine f
            JOIN Member m ON m.MemberID = f.MemberID
            JOIN AppUser u ON u.UserID = m.UserID
            OUTER APPLY (SELECT SUM(AmountPaid) AS CollectedAmt FROM FinePayment
                         WHERE FineID = f.FineID AND PaymentStatus = N'Completed') p
            OUTER APPLY (SELECT SUM(ApprovedReduction) AS ReductionAmt FROM FineAppeal
                         WHERE FineID = f.FineID AND Status = N'Approved') r
            WHERE f.Status NOT IN (N'Waived', N'Fully Paid')
            ORDER BY Balance DESC
            """, nativeQuery = true)
    List<Object[]> topOutstandingFines();

    /** Finance dashboard chart: completed payments summed per calendar week, over the given window. */
    @Query(value = """
            SELECT CAST(DATEADD(DAY, -(DATEPART(WEEKDAY, PaidAt) - 1), PaidAt) AS DATE) AS WeekStart, SUM(AmountPaid) AS Collected
            FROM FinePayment
            WHERE PaymentStatus = N'Completed' AND PaidAt BETWEEN :from AND :to
            GROUP BY CAST(DATEADD(DAY, -(DATEPART(WEEKDAY, PaidAt) - 1), PaidAt) AS DATE)
            ORDER BY WeekStart
            """, nativeQuery = true)
    List<Object[]> collectionsPerWeek(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
