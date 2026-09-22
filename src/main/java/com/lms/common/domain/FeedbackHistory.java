package com.lms.common.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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
 * Table {@code FeedbackHistory} — entity FEEDBACK_HISTORY (1:N STATUS
 * HISTORY). Part of the {@link MemberFeedback} aggregate. One row per change
 * of status or response.
 */
@Entity
@Table(name = "FeedbackHistory")
@Getter
@Setter
@NoArgsConstructor
public class FeedbackHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FeedbackHistoryID")
    @Setter(AccessLevel.NONE)
    private Integer feedbackHistoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FeedbackID", nullable = false)
    private MemberFeedback feedback;

    @Convert(converter = FeedbackStatus.JpaConverter.class)
    @Column(name = "PreviousStatus", nullable = false, length = 15)
    private FeedbackStatus previousStatus;

    @Convert(converter = FeedbackStatus.JpaConverter.class)
    @Column(name = "NewStatus", nullable = false, length = 15)
    private FeedbackStatus newStatus;

    @Column(name = "PreviousResponse", length = 2000)
    private String previousResponse;

    @Column(name = "NewResponse", length = 2000)
    private String newResponse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ChangedByUserID", nullable = false)
    private AppUser changedBy;

    @Column(name = "ChangedAt", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    void onCreate() {
        if (changedAt == null) {
            changedAt = DbTime.now();
        }
    }
}
