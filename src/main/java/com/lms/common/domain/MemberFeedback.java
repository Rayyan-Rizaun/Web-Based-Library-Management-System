package com.lms.common.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
 * Table {@code MemberFeedback} — entity MEMBER_FEEDBACK (1:N SUBMITS,
 * BELONGS TO). Aggregate root for {@link FeedbackHistory}.
 */
@Entity
@Table(name = "MemberFeedback")
@Getter
@Setter
@NoArgsConstructor
public class MemberFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FeedbackID")
    @Setter(AccessLevel.NONE)
    private Integer feedbackId;

    /** The unique reference number shown to the member. */
    @Column(name = "FeedbackReference", nullable = false, length = 20)
    private String feedbackReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MemberID", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FeedbackCategoryID", nullable = false)
    private FeedbackCategory category;

    @Column(name = "Subject", nullable = false, length = 150)
    private String subject;

    @Column(name = "Description", nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "Priority", nullable = false, length = 10)
    private FeedbackPriority priority = FeedbackPriority.Medium;

    @Convert(converter = FeedbackStatus.JpaConverter.class)
    @Column(name = "Status", nullable = false, length = 15)
    private FeedbackStatus status = FeedbackStatus.Submitted;

    @Column(name = "AdminResponse", length = 2000)
    private String adminResponse;

    @Column(name = "SubmittedAt", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Table FeedbackHistory. The database deletes these rows itself when the
     * feedback is withdrawn (FK_FeedbackHistory_MemberFeedback ON DELETE CASCADE).
     */
    @OneToMany(mappedBy = "feedback", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("changedAt ASC")
    @Setter(AccessLevel.NONE)
    private List<FeedbackHistory> history = new ArrayList<>();

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
