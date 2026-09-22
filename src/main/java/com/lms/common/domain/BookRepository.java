package com.lms.common.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Aggregate root {@link Book}, including its authors, categories and keywords.
 * UC-02 (manage catalogue) and online search by title, author, category or keyword.
 */
public interface BookRepository extends JpaRepository<Book, Integer> {

    /** UC-02: a duplicate ISBN is an error. */
    Optional<Book> findByIsbn13(String isbn13);

    boolean existsByIsbn13(String isbn13);

    /** UC-02: the filtered UNIQUE index UX_Book_ISBN10 allows this to duplicate only when both are NULL. */
    boolean existsByIsbn10(String isbn10);

    Page<Book> findByActiveTrueAndTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Book> findDistinctByActiveTrueAndAuthorsAuthorAuthorNameContainingIgnoreCase(String authorName, Pageable pageable);

    Page<Book> findDistinctByActiveTrueAndCategoriesCategoryId(Integer categoryId, Pageable pageable);

    /** Exact keyword match against table BookKeyword. */
    List<Book> findDistinctByActiveTrueAndKeywords(String keyword);

    /**
     * The catalogue list/search screen (UC-02, PB-22/PB-23): one search box
     * matched against title, ISBN-13, ISBN-10, author name and keyword all
     * at once, an optional category filter, and an optional "available
     * copies only" filter — all applied together, with paging and sorting.
     * None of the single-field finder methods above can express that
     * combination, so this is one added {@code @Query} rather than a chain
     * of separate calls (which cannot honour "search AND filter AND sort"
     * as one request). {@code com.lms.catalogue.BookService} builds every
     * argument (lower-cased {@code %pattern%} for {@code likeQuery}, or
     * {@code null} to skip that filter) so this method stays a plain query,
     * not a place business rules live (CLAUDE.md rule 5).
     *
     * <p>{@code availableStatus} is always passed as {@code
     * BookCopyStatus.Available} — bound as a bean parameter, not inlined as
     * a JPQL enum literal, so Hibernate applies {@code
     * BookCopyStatus.JpaConverter} the same way it would for any other
     * query. It is only actually compared when {@code availableOnly} is
     * true; the short-circuit keeps the query correct either way.
     */
    @Query(value = """
            SELECT DISTINCT b FROM Book b
            LEFT JOIN b.authors ba
            LEFT JOIN ba.author a
            WHERE b.active = true
              AND (:likeQuery IS NULL
                   OR LOWER(b.title) LIKE :likeQuery
                   OR LOWER(b.isbn13) LIKE :likeQuery
                   OR LOWER(b.isbn10) LIKE :likeQuery
                   OR LOWER(a.authorName) LIKE :likeQuery
                   OR EXISTS (SELECT 1 FROM b.keywords kw WHERE LOWER(kw) LIKE :likeQuery))
              AND (:categoryId IS NULL
                   OR EXISTS (SELECT 1 FROM b.categories c WHERE c.categoryId = :categoryId))
              AND (:availableOnly = false
                   OR EXISTS (SELECT 1 FROM BookCopy bc WHERE bc.book = b AND bc.status = :availableStatus))
            """,
            countQuery = """
            SELECT COUNT(DISTINCT b) FROM Book b
            LEFT JOIN b.authors ba
            LEFT JOIN ba.author a
            WHERE b.active = true
              AND (:likeQuery IS NULL
                   OR LOWER(b.title) LIKE :likeQuery
                   OR LOWER(b.isbn13) LIKE :likeQuery
                   OR LOWER(b.isbn10) LIKE :likeQuery
                   OR LOWER(a.authorName) LIKE :likeQuery
                   OR EXISTS (SELECT 1 FROM b.keywords kw WHERE LOWER(kw) LIKE :likeQuery))
              AND (:categoryId IS NULL
                   OR EXISTS (SELECT 1 FROM b.categories c WHERE c.categoryId = :categoryId))
              AND (:availableOnly = false
                   OR EXISTS (SELECT 1 FROM BookCopy bc WHERE bc.book = b AND bc.status = :availableStatus))
            """)
    Page<Book> search(@Param("likeQuery") String likeQuery,
                       @Param("categoryId") Integer categoryId,
                       @Param("availableOnly") boolean availableOnly,
                       @Param("availableStatus") BookCopyStatus availableStatus,
                       Pageable pageable);
}
