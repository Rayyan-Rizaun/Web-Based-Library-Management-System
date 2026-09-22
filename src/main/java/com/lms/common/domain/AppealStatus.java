package com.lms.common.domain;

/**
 * {@code FineAppeal.Status} — CK_FineAppeal_Status.
 *
 * <p>Constant names are the stored values, so the column is mapped with
 * {@code @Enumerated(EnumType.STRING)}.
 */
public enum AppealStatus {
    Pending, Approved, Rejected
}
