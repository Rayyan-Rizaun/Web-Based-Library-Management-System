package com.lms.common.domain;

import java.time.LocalDateTime;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table {@code BookIncident} — entity BOOK_INCIDENT: a lost or damaged copy.
 *
 * <p>{@link #copy} and {@link #member} are kept even when {@link #loan} is
 * set, because the loan is optional — damage found on the shelf has no loan
 * (R20). The charge itself is not stored here; it is the linked
 * {@link Fine}'s AmountAssessed (R21).
 */
@Entity
@Table(name = "BookIncident")
@Getter
@Setter
@NoArgsConstructor
public class BookIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IncidentID")
    @Setter(AccessLevel.NONE)
    private Integer incidentId;

    /** AFFECTS. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CopyID", nullable = false)
    private BookCopy copy;

    /** ARISES FROM — optional. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LoanID")
    private Loan loan;

    /** LIABLE MEMBER — optional. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MemberID")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RecordedByStaffID", nullable = false)
    private StaffProfile recordedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "IncidentType", nullable = false, length = 10)
    private IncidentType incidentType;

    /** The librarian's justification note (business-rules §5). */
    @Column(name = "Description", nullable = false, length = 1000)
    private String description;

    @Column(name = "ReportedAt", nullable = false, updatable = false)
    private LocalDateTime reportedAt;

    @Convert(converter = IncidentStatus.JpaConverter.class)
    @Column(name = "Status", nullable = false, length = 15)
    private IncidentStatus status = IncidentStatus.Open;

    @PrePersist
    void onCreate() {
        if (reportedAt == null) {
            reportedAt = DbTime.now();
        }
    }
}
