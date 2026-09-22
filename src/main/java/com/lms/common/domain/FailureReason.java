package com.lms.common.domain;

/**
 * {@code FailedLoginAttempt.FailureReason} — CK_FailedLoginAttempt_FailureReason.
 *
 * <p>Constant names are the stored values, so the column is mapped with
 * {@code @Enumerated(EnumType.STRING)}.
 */
public enum FailureReason {
    UnknownEmail, BadPassword, AccountLocked, AccountDeactivated
}
