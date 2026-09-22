package com.lms.common.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Aggregate root {@link BookCopy}. UC-02 inventory, UC-03 issue/return, UC-04 "is a copy available?".
 */
public interface BookCopyRepository extends JpaRepository<BookCopy, Integer> {

    List<BookCopy> findByBookBookIdOrderByAccessionNumber(Integer bookId);

    /** Total copies of a title, any status — the book list's "of N copies" figure. */
    long countByBookBookId(Integer bookId);

    /** Availability count for a title, e.g. with {@code BookCopyStatus.Available}. */
    long countByBookBookIdAndStatus(Integer bookId, BookCopyStatus status);

    /** UC-04: if a lendable copy is available, suggest borrowing instead of reserving. */
    boolean existsByBookBookIdAndStatusAndReferenceOnlyFalse(Integer bookId, BookCopyStatus status);

    /** UC-03: pick a lendable copy of a title to issue. */
    Optional<BookCopy> findFirstByBookBookIdAndStatusAndReferenceOnlyFalseOrderByAccessionNumber(
            Integer bookId, BookCopyStatus status);

    /** UC-03: scanning a copy at the desk. */
    Optional<BookCopy> findByBarcode(String barcode);

    Optional<BookCopy> findByAccessionNumber(String accessionNumber);

    boolean existsByBarcode(String barcode);

    boolean existsByAccessionNumber(String accessionNumber);

    @Query(value = """
            SELECT c.Status AS Status, COUNT(*) AS Cnt
            FROM BookCopy c
            GROUP BY c.Status
            ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<Object[]> countGroupedByStatus();

    @Query(value = """
            SELECT b.Title AS BookTitle, COUNT(c.CopyID) AS TotalCopies
            FROM Book b
            JOIN BookCopy c ON c.BookID = b.BookID
            GROUP BY b.BookID, b.Title
            HAVING SUM(CASE WHEN c.Status = N'Available' AND c.IsReferenceOnly = 0 THEN 1 ELSE 0 END) = 0
            ORDER BY b.Title ASC
            """, nativeQuery = true)
    List<Object[]> titlesWithNothingAvailable();

    @Query(value = """
            SELECT COUNT(*) AS TotalCopies,
                   SUM(CASE WHEN c.Status = N'Available' THEN 1 ELSE 0 END) AS AvailableNow
            FROM BookCopy c
            """, nativeQuery = true)
    List<Object[]> inventorySummary();
}
