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

/** Table {@code FinePayment} — entity FINE_PAYMENT (1:N PARTIAL/FULL PAYMENTS). Part of the {@link Fine} aggregate. */
@Entity
@Table(name = "FinePayment")
@Getter
@Setter
@NoArgsConstructor
public class FinePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PaymentID")
    @Setter(AccessLevel.NONE)
    private Integer paymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FineID", nullable = false)
    private Fine fine;

    /** Unique receipt number (UC-07). */
    @Column(name = "ReceiptNumber", nullable = false, length = 30)
    private String receiptNumber;

    @Column(name = "AmountPaid", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Enumerated(EnumType.STRING)
    @Column(name = "PaymentMethod", nullable = false, length = 10)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "PaymentStatus", nullable = false, length = 10)
    private PaymentStatus paymentStatus = PaymentStatus.Completed;

    @Column(name = "PaidAt", nullable = false)
    private LocalDateTime paidAt;

    /** NULL for online self-payment; required for Cash (CK_FinePayment_CashHasReceiver). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReceivedByStaffID")
    private StaffProfile receivedBy;

    @PrePersist
    void onCreate() {
        if (paidAt == null) {
            paidAt = DbTime.now();
        }
    }
}
