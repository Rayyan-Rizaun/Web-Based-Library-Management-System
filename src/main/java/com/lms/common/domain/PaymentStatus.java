package com.lms.common.domain;

/**
 * {@code FinePayment.PaymentStatus} — CK_FinePayment_PaymentStatus.
 *
 * <p>Constant names are the stored values, so the column is mapped with
 * {@code @Enumerated(EnumType.STRING)}.
 */
public enum PaymentStatus {
    Completed, Failed, Refunded
}
