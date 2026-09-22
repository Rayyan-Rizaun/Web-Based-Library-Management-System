package com.lms.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Table {@code FeedbackCategory} — entity FEEDBACK_CATEGORY (not the same as book {@link Category}). */
@Entity
@Table(name = "FeedbackCategory")
@Getter
@Setter
@NoArgsConstructor
public class FeedbackCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FeedbackCategoryID")
    @Setter(AccessLevel.NONE)
    private Integer feedbackCategoryId;

    @Column(name = "CategoryName", nullable = false, length = 50)
    private String categoryName;

    @Column(name = "IsActive", nullable = false)
    private boolean active = true;
}
