package com.lms.common.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table {@code BookReview} — entity BOOK_REVIEW. Aggregate root for
 * {@link ReviewFlag} and {@link ReviewModerationHistory}.
 *
 * <p>No ModeratedBy field (R22): moderators are recorded per transition in
 * {@link #moderationHistory}.
 */
@Entity
@Table(name = "BookReview")
@Getter
@Setter
@NoArgsConstructor
public class BookReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReviewID")
    @Setter(AccessLevel.NONE)
    private Integer reviewId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BookID", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MemberID", nullable = false)
    private Member member;

    /** 1 to 5. TINYINT — declared explicitly because Java has no unsigned byte type. */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "Rating", nullable = false)
    private Integer rating;

    /** Optional written review. */
    @Column(name = "ReviewText", length = 2000)
    private String reviewText;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 10)
    private ReviewStatus status = ReviewStatus.Pending;

    @Column(name = "SubmittedAt", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    /** Table ReviewFlag. */
    @OneToMany(mappedBy = "review", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("reportedAt ASC")
    @Setter(AccessLevel.NONE)
    private List<ReviewFlag> flags = new ArrayList<>();

    /** Table ReviewModerationHistory. */
    @OneToMany(mappedBy = "review", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("moderatedAt ASC")
    @Setter(AccessLevel.NONE)
    private List<ReviewModerationHistory> moderationHistory = new ArrayList<>();

    @PrePersist
    void onCreate() {
        LocalDateTime now = DbTime.now();
        if (submittedAt == null) {
            submittedAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = DbTime.now();
    }
}
