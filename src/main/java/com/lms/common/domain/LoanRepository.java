package com.lms.common.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Aggregate root {@link Loan}, including its {@link LoanRenewal}s.
 * UC-03 (borrow, return, renew), UC-02's "no active loans" delete guard, UC-09's
 * "has borrowed this book" check, and UC-08 circulation reports.
 *
 * <p>"Active" below means {@code LoanStatus.Active}. A loan is overdue when it is
 * Active and its due date has passed — that state is never stored (R15).
 */
public interface LoanRepository extends JpaRepository<Loan, Integer> {

    /** A member's current loans, soonest due first. */
    List<Loan> findByMemberMemberIdAndStatusOrderByDueAtAsc(Integer memberId, LoanStatus status);

    /** Every current loan across all members, soonest due first — UC-03's "Current Loans" screen. */
    List<Loan> findByStatusOrderByDueAtAsc(LoanStatus status);

    /** Borrowing limit check (the limit itself is a SystemSetting). */
    long countByMemberMemberIdAndStatus(Integer memberId, LoanStatus status);

    /** Borrowing block: does the member hold an Active loan due before {@code now}? */
    boolean existsByMemberMemberIdAndStatusAndDueAtBefore(Integer memberId, LoanStatus status, LocalDateTime now);

    /** Dashboard stat tiles: how many loans are currently in this state, system-wide. */
    long countByStatus(LoanStatus status);

    /** Dashboard stat tiles: overdue loans, system-wide (Active, past due, never returned). */
    long countByStatusAndDueAtBefore(LoanStatus status, LocalDateTime now);

    /** Dashboard list: the most overdue loans, system-wide. */
    List<Loan> findTop5ByStatusAndDueAtBeforeOrderByDueAtAsc(LoanStatus status, LocalDateTime now);

    /** Librarian dashboard: books issued in a window (e.g. today). */
    long countByBorrowedAtBetween(LocalDateTime from, LocalDateTime to);

    /** Librarian dashboard: returns due in a window (e.g. today). */
    long countByStatusAndDueAtBetween(LoanStatus status, LocalDateTime from, LocalDateTime to);

    /** Member dashboard: this member's own loans due in a window (e.g. the next 3 days). */
    long countByMemberMemberIdAndStatusAndDueAtBetween(Integer memberId, LoanStatus status, LocalDateTime from, LocalDateTime to);

    /** UC-03 return: the open loan for a copy. */
    Optional<Loan> findByCopyCopyIdAndStatus(Integer copyId, LoanStatus status);

    Optional<Loan> findByCopyBarcodeAndStatus(String barcode, LoanStatus status);

    /** Borrowing history, newest first. */
    Page<Loan> findByMemberMemberIdOrderByBorrowedAtDesc(Integer memberId, Pageable pageable);

    /** Overdue list and reminders: Active loans due before {@code now}. */
    List<Loan> findByStatusAndDueAtBeforeOrderByDueAtAsc(LoanStatus status, LocalDateTime now);

    /** "Due soon" reminders. */
    List<Loan> findByStatusAndDueAtBetweenOrderByDueAtAsc(LoanStatus status, LocalDateTime from, LocalDateTime to);

    /** UC-02: a book may only be deleted when none of its copies is on an Active loan. */
    boolean existsByCopyBookBookIdAndStatus(Integer bookId, LoanStatus status);

    /** UC-09 precondition: has this member ever borrowed any copy of this book? */
    boolean existsByMemberMemberIdAndCopyBookBookId(Integer memberId, Integer bookId);

    /** UC-08 borrowed-books report. */
    List<Loan> findByBorrowedAtBetweenOrderByBorrowedAtAsc(LocalDateTime from, LocalDateTime to);

    /** UC-08 returned-books report. */
    List<Loan> findByReturnedAtBetweenOrderByReturnedAtAsc(LocalDateTime from, LocalDateTime to);

    /**
     * UC-03 "Loan History": one search box matched against the borrowing
     * member's first/last name and the book's title at once, a fixed
     * {@code status} filter (always {@code Returned} from the caller, bound
     * as a parameter rather than inlined — the same reason {@code
     * BookRepository.search} binds its own status filter — plus paging.
     * {@code com.lms.borrowing.BorrowingService} builds {@code likeQuery}
     * (a lower-cased {@code %pattern%}, or {@code null} to skip it), so
     * this stays a plain query, not a place business rules live.
     */
    @Query(value = """
            SELECT l FROM Loan l
            JOIN l.member m
            JOIN m.user u
            JOIN l.copy c
            JOIN c.book b
            WHERE l.status = :status
              AND (:likeQuery IS NULL
                   OR LOWER(u.firstName) LIKE :likeQuery
                   OR LOWER(u.lastName) LIKE :likeQuery
                   OR LOWER(b.title) LIKE :likeQuery)
            """,
            countQuery = """
            SELECT COUNT(l) FROM Loan l
            JOIN l.member m
            JOIN m.user u
            JOIN l.copy c
            JOIN c.book b
            WHERE l.status = :status
              AND (:likeQuery IS NULL
                   OR LOWER(u.firstName) LIKE :likeQuery
                   OR LOWER(u.lastName) LIKE :likeQuery
                   OR LOWER(b.title) LIKE :likeQuery)
            """)
    Page<Loan> search(@Param("status") LoanStatus status, @Param("likeQuery") String likeQuery, Pageable pageable);

    @Query(value = """
            SELECT m.MemberType AS MemberType, c.CategoryName AS CategoryName, COUNT(*) AS LoanCount
            FROM Loan l
            JOIN Member m ON m.MemberID = l.MemberID
            JOIN BookCopy bc ON bc.CopyID = l.CopyID
            JOIN BookCategory bcat ON bcat.BookID = bc.BookID
            JOIN Category c ON c.CategoryID = bcat.CategoryID
            WHERE l.BorrowedAt BETWEEN :from AND :to
              AND (:memberType IS NULL OR m.MemberType = :memberType)
              AND (:categoryId IS NULL OR c.CategoryID = :categoryId)
            GROUP BY m.MemberType, c.CategoryName
            ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<Object[]> borrowingByMemberTypeAndCategory(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
            @Param("memberType") String memberType, @Param("categoryId") Integer categoryId);

    @Query(value = """
            SELECT COUNT(*) AS TotalLoans, COUNT(DISTINCT l.MemberID) AS UniqueMembers, COUNT(DISTINCT bc.BookID) AS UniqueTitles
            FROM Loan l
            JOIN BookCopy bc ON bc.CopyID = l.CopyID
            JOIN Member m ON m.MemberID = l.MemberID
            WHERE l.BorrowedAt BETWEEN :from AND :to
              AND (:memberType IS NULL OR m.MemberType = :memberType)
              AND (:categoryId IS NULL OR EXISTS (SELECT 1 FROM BookCategory bcat WHERE bcat.BookID = bc.BookID AND bcat.CategoryID = :categoryId))
            """, nativeQuery = true)
    List<Object[]> borrowingSummary(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
            @Param("memberType") String memberType, @Param("categoryId") Integer categoryId);

    @Query(value = """
            SELECT u.FirstName + N' ' + u.LastName AS MemberName, b.Title AS BookTitle, l.DueAt AS DueAt,
                   DATEDIFF(DAY, l.DueAt, SYSDATETIME()) AS DaysOverdue
            FROM Loan l
            JOIN Member m ON m.MemberID = l.MemberID
            JOIN AppUser u ON u.UserID = m.UserID
            JOIN BookCopy bc ON bc.CopyID = l.CopyID
            JOIN Book b ON b.BookID = bc.BookID
            WHERE l.Status = N'Active' AND l.ReturnedAt IS NULL AND l.DueAt < SYSDATETIME()
              AND l.DueAt BETWEEN :from AND :to
              AND (:memberType IS NULL OR m.MemberType = :memberType)
            ORDER BY l.DueAt ASC
            """, nativeQuery = true)
    List<Object[]> overdueRows(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
            @Param("memberType") String memberType);

    @Query(value = """
            SELECT COUNT(*) AS OverdueCount,
                   AVG(CAST(DATEDIFF(DAY, l.DueAt, SYSDATETIME()) AS FLOAT)) AS AvgDays,
                   SUM(CASE WHEN DATEDIFF(DAY, l.DueAt, SYSDATETIME()) * :rate > :cap THEN :cap
                            ELSE DATEDIFF(DAY, l.DueAt, SYSDATETIME()) * :rate END) AS TotalFine
            FROM Loan l
            JOIN Member m ON m.MemberID = l.MemberID
            WHERE l.Status = N'Active' AND l.ReturnedAt IS NULL AND l.DueAt < SYSDATETIME()
              AND l.DueAt BETWEEN :from AND :to
              AND (:memberType IS NULL OR m.MemberType = :memberType)
            """, nativeQuery = true)
    List<Object[]> overdueSummary(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
            @Param("memberType") String memberType, @Param("rate") BigDecimal rate, @Param("cap") BigDecimal cap);

    @Query(value = """
            SELECT TOP 20 b.Title AS BookTitle, COUNT(*) AS TimesBorrowed
            FROM Loan l
            JOIN BookCopy bc ON bc.CopyID = l.CopyID
            JOIN Book b ON b.BookID = bc.BookID
            WHERE l.BorrowedAt BETWEEN :from AND :to
              AND (:categoryId IS NULL OR EXISTS (SELECT 1 FROM BookCategory bcat WHERE bcat.BookID = b.BookID AND bcat.CategoryID = :categoryId))
            GROUP BY b.BookID, b.Title
            ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<Object[]> mostBorrowedTitles(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
            @Param("categoryId") Integer categoryId);

    /** Admin dashboard chart: loans issued per calendar month, over the given window. */
    @Query(value = """
            SELECT CAST(DATEFROMPARTS(YEAR(BorrowedAt), MONTH(BorrowedAt), 1) AS DATE) AS MonthStart, COUNT(*) AS LoanCount
            FROM Loan
            WHERE BorrowedAt BETWEEN :from AND :to
            GROUP BY YEAR(BorrowedAt), MONTH(BorrowedAt)
            ORDER BY YEAR(BorrowedAt), MONTH(BorrowedAt)
            """, nativeQuery = true)
    List<Object[]> loansPerMonth(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Librarian dashboard chart: loans issued per calendar day, over the given window. */
    @Query(value = """
            SELECT CAST(BorrowedAt AS DATE) AS LoanDay, COUNT(*) AS LoanCount
            FROM Loan
            WHERE BorrowedAt BETWEEN :from AND :to
            GROUP BY CAST(BorrowedAt AS DATE)
            ORDER BY CAST(BorrowedAt AS DATE)
            """, nativeQuery = true)
    List<Object[]> loansPerDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
