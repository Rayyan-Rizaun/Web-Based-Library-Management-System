package com.lms.common.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Aggregate root {@link Author}. UC-02 catalogue data entry and author search.
 */
public interface AuthorRepository extends JpaRepository<Author, Integer> {

    Optional<Author> findByAuthorNameIgnoreCase(String authorName);

    List<Author> findByAuthorNameContainingIgnoreCaseOrderByAuthorName(String authorName);

    /** Same shape as CategoryRepository/PublisherRepository's active-only list, for the Book form's author dropdown. */
    List<Author> findByActiveTrueOrderByAuthorName();
}
