package com.lms.common.domain;

import java.math.BigDecimal;
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
 * Table {@code FineAppeal} — entity FINE_APPEAL (1:N APPEALS). Part of the
 * {@link Fine} aggregate.
 *
 * <p>No member field (R19): the appellant is always the fine's member,
 * {@code getFine().getMember()}.
 */
@Entity
@Table(name = "FineAppeal")
@Getter
@Setter
@NoArgsConstructor
public class FineAppeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AppealID")
    @Setter(AccessLevel.NONE)
    private Integer appealId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FineID", nullable = false)
    private Fine fine;

    @Column(name = "AppealReason", nullable = false, length = 1000)
    private String appealReason;

    @Column(name = "SubmittedAt", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 10)
    private AppealStatus status = AppealStatus.Pending;

    /** Set with {@link #decidedAt} once the status leaves Pending (CK_FineAppeal_DecisionMatchesStatus). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DecidedByStaffID")
    private StaffProfile decidedBy;

    @Column(name = "DecidedAt")
    private LocalDateTime decidedAt;

    @Column(name = "DecisionComments", length = 1000)
    private String decisionComments;

    /** Required when Approved, NULL otherwise. */
    @Column(name = "ApprovedReduction", precision = 10, scale = 2)
    private BigDecimal approvedReduction;

    @PrePersist
    void onCreate() {
        if (submittedAt == null) {
            submittedAt = DbTime.now();
        }
    }
}
