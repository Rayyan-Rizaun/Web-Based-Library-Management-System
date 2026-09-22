package com.lms.common.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table {@code BookCopy} — entity BOOK_COPY, one physical copy of a
 * {@link Book} (1:N COPIES OF). Its own aggregate root: copies are added,
 * loaned and written off independently of the title.
 */
@Entity
@Table(name = "BookCopy")
@Getter
@Setter
@NoArgsConstructor
public class BookCopy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CopyID")
    @Setter(AccessLevel.NONE)
    private Integer copyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BookID", nullable = false)
    private Book book;

    @Column(name = "AccessionNumber", nullable = false, length = 30)
    private String accessionNumber;

    @Column(name = "Barcode", nullable = false, length = 50)
    private String barcode;

    @Column(name = "ShelfLocation", length = 50)
    private String shelfLocation;

    @Column(name = "AcquisitionDate", nullable = false)
    private LocalDate acquisitionDate;

    /** Per-copy replacement cost used for lost/damaged charges (business-rules §5). */
    @Column(name = "PurchasePrice", nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "CopyCondition", nullable = false, length = 10)
    private CopyCondition copyCondition = CopyCondition.Good;

    /** Kept status column; the D5 triggers are its only intended writers (R24). */
    @Convert(converter = BookCopyStatus.JpaConverter.class)
    @Column(name = "Status", nullable = false, length = 20)
    private BookCopyStatus status = BookCopyStatus.Available;

    @Column(name = "IsReferenceOnly", nullable = false)
    private boolean referenceOnly = false;
}
