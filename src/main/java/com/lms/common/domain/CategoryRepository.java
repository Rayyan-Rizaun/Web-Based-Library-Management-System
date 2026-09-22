package com.lms.common.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Aggregate root {@link Category} (book categories). UC-02 and the search filters.
 */
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    Optional<Category> findByCategoryNameIgnoreCase(String categoryName);

    List<Category> findByActiveTrueOrderByCategoryName();
}
