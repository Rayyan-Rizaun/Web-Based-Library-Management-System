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

/** Table {@code ReviewFlag} — entity REVIEW_FLAG (1:N FLAGS, REPORTED BY). Part of the {@link BookReview} aggregate. */
@Entity
@Table(name = "ReviewFlag")
@Getter
@Setter
@NoArgsConstructor
public class ReviewFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReviewFlagID")
    @Setter(AccessLevel.NONE)
    private Integer reviewFlagId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ReviewID", nullable = false)
    private BookReview review;

    /** A different member from the review's author (D5 trigger). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ReportedByMemberID", nullable = false)
    private Member reportedBy;

    @Column(name = "Reason", nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 10)
    private FlagStatus status = FlagStatus.Open;

    @Column(name = "ReportedAt", nullable = false, updatable = false)
    private LocalDateTime reportedAt;

    @PrePersist
    void onCreate() {
        if (reportedAt == null) {
            reportedAt = DbTime.now();
        }
    }
}
