package com.lms.common.domain;

/**
 * {@code LoanRenewal.Status} — CK_LoanRenewal_Status.
 *
 * <p>Constant names are the stored values, so the column is mapped with
 * {@code @Enumerated(EnumType.STRING)}.
 */
public enum RenewalStatus {
    Pending, Approved, Rejected
}
