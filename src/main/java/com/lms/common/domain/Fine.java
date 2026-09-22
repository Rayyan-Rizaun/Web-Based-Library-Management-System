package com.lms.common.domain;

import java.math.BigDecimal;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table {@code Fine} — entity FINE. Aggregate root for {@link FineAppeal}
 * and {@link FinePayment}.
 *
 * <p>The source is exactly one of: a {@link #loan} (FineType Overdue) or an
 * {@link #incident} (Lost / Damaged) — CK_Fine_SourceMatchesType.
 *
 * <p>Not stored, by design: DaysOverdue (R16), WaivedAmount (R17) and the
 * outstanding balance (R18), which is AmountAssessed − approved appeal
 * reductions − completed payments.
 */
@Entity
@Table(name = "Fine")
@Getter
@Setter
@NoArgsConstructor
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FineID")
    @Setter(AccessLevel.NONE)
    private Integer fineId;

    /** CHARGED TO. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MemberID", nullable = false)
    private Member member;

    /** OVERDUE SOURCE — set only for FineType Overdue. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LoanID")
    private Loan loan;

    /** LOSS/DAMAGE SOURCE — set only for FineType Lost or Damaged. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IncidentID")
    private BookIncident incident;

    @Enumerated(EnumType.STRING)
    @Column(name = "FineType", nullable = false, length = 10)
    private FineType fineType;

    /** Snapshot of 'Fine.RatePerDay' at assessment; Overdue fines only. */
    @Column(name = "RatePerDay", precision = 10, scale = 2)
    private BigDecimal ratePerDay;

    @Column(name = "AmountAssessed", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountAssessed;

    @Column(name = "AssessedAt", nullable = false, updatable = false)
    private LocalDateTime assessedAt;

    /** Kept status column; the D5 triggers are its only intended writers (R24). */
    @Convert(converter = FineStatus.JpaConverter.class)
    @Column(name = "Status", nullable = false, length = 15)
    private FineStatus status = FineStatus.Pending;

    /** Required when status is Waived, NULL otherwise (CK_Fine_WaiverDetails). */
    @Column(name = "WaiverReason", length = 500)
    private String waiverReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WaivedByStaffID")
    private StaffProfile waivedBy;

    /** Table FineAppeal. */
    @OneToMany(mappedBy = "fine", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("submittedAt ASC")
    @Setter(AccessLevel.NONE)
    private List<FineAppeal> appeals = new ArrayList<>();

    /** Table FinePayment. */
    @OneToMany(mappedBy = "fine", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("paidAt ASC")
    @Setter(AccessLevel.NONE)
    private List<FinePayment> payments = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (assessedAt == null) {
            assessedAt = DbTime.now();
        }
    }
}
