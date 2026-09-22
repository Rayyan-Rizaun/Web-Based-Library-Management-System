package com.lms.common.domain;

import java.time.LocalDateTime;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table {@code ReviewModerationHistory} — entity REVIEW_MODERATION_HISTORY
 * (1:N MODERATION HISTORY). Part of the {@link BookReview} aggregate. One row
 * per status change; rows are never edited or deleted.
 */
@Entity
@Table(name = "ReviewModerationHistory")
@Getter
@Setter
@NoArgsConstructor
public class ReviewModerationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ModerationID")
    @Setter(AccessLevel.NONE)
    private Integer moderationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ReviewID", nullable = false)
    private BookReview review;

    @Enumerated(EnumType.STRING)
    @Column(name = "PreviousStatus", nullable = false, length = 10)
    private ReviewStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "NewStatus", nullable = false, length = 10)
    private ReviewStatus newStatus;

    /** Required when the new status is Rejected (business-rules §6). */
    @Column(name = "Reason", length = 500)
    private String reason;

    /** NULL only for the automatic hide when a review is flagged. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ModeratorStaffID")
    private StaffProfile moderator;

    @Column(name = "ModeratedAt", nullable = false, updatable = false)
    private LocalDateTime moderatedAt;

    @PrePersist
    void onCreate() {
        if (moderatedAt == null) {
            moderatedAt = DbTime.now();
        }
    }
}
