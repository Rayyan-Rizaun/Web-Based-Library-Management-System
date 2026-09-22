package com.lms.common.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table {@code Loan} — the aggregation MEMBER—BORROWS—BOOK_COPY
 * (00_relational_mapping.md §2). Aggregate root for {@link LoanRenewal}.
 *
 * <p>No RenewalCount field (R14): count the approved entries in
 * {@link #renewals}. No "Overdue" status (R15): a loan is overdue when
 * {@code status == Active} and {@code dueAt} is in the past.
 */
@Entity
@Table(name = "Loan")
@Getter
@Setter
@NoArgsConstructor
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LoanID")
    @Setter(AccessLevel.NONE)
    private Integer loanId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MemberID", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CopyID", nullable = false)
    private BookCopy copy;

    /** ISSUED BY — a staff profile, never a member-only account (R27). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IssuedByStaffID", nullable = false)
    private StaffProfile issuedBy;

    /** RETURNED BY — set together with {@link #returnedAt} and {@link #returnCondition}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReturnedToStaffID")
    private StaffProfile returnedTo;

    @Column(name = "BorrowedAt", nullable = false, updatable = false)
    private LocalDateTime borrowedAt;

    @Column(name = "DueAt", nullable = false)
    private LocalDateTime dueAt;

    @Column(name = "ReturnedAt")
    private LocalDateTime returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "ReturnCondition", length = 10)
    private ReturnCondition returnCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 10)
    private LoanStatus status = LoanStatus.Active;

    /** Table LoanRenewal. History rows: saved with the loan, never deleted through it. */
    @OneToMany(mappedBy = "loan", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("requestedAt ASC")
    @Setter(AccessLevel.NONE)
    private List<LoanRenewal> renewals = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (borrowedAt == null) {
            borrowedAt = DbTime.now();
        }
    }
}
