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

/** Table {@code LoanRenewal} — entity LOAN_RENEWAL (1:N RENEWALS). Part of the {@link Loan} aggregate. */
@Entity
@Table(name = "LoanRenewal")
@Getter
@Setter
@NoArgsConstructor
public class LoanRenewal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RenewalID")
    @Setter(AccessLevel.NONE)
    private Integer renewalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LoanID", nullable = false)
    private Loan loan;

    /** The member, or a librarian acting on their behalf — hence AppUser, not Member. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RequestedByUserID", nullable = false)
    private AppUser requestedBy;

    @Column(name = "RequestedAt", nullable = false)
    private LocalDateTime requestedAt;

    /** NULL when the system approves automatically. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApprovedByStaffID")
    private StaffProfile approvedBy;

    @Column(name = "OldDueAt", nullable = false)
    private LocalDateTime oldDueAt;

    @Column(name = "NewDueAt", nullable = false)
    private LocalDateTime newDueAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 10)
    private RenewalStatus status = RenewalStatus.Pending;

    @PrePersist
    void onCreate() {
        if (requestedAt == null) {
            requestedAt = DbTime.now();
        }
    }
}
