package com.lms.common.domain;

/**
 * {@code Loan.Status} — CK_Loan_Status.
 * "Overdue" is not stored; it is ReturnedAt IS NULL AND DueAt &lt; now (R15).
 *
 * <p>Constant names are the stored values, so the column is mapped with
 * {@code @Enumerated(EnumType.STRING)}.
 */
public enum LoanStatus {
    Active, Returned, Lost
}
