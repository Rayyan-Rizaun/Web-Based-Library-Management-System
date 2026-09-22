package com.lms.common.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Aggregate root {@link FeedbackCategory}. UC-10 category picker.
 */
public interface FeedbackCategoryRepository extends JpaRepository<FeedbackCategory, Integer> {

    Optional<FeedbackCategory> findByCategoryNameIgnoreCase(String categoryName);

    List<FeedbackCategory> findByActiveTrueOrderByCategoryName();

    /** Staff filter dropdown: includes inactive categories too, so old feedback against a since-retired category can still be filtered on. */
    List<FeedbackCategory> findAllByOrderByCategoryName();
}
