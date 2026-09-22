package com.lms.common.domain;

/**
 * {@code Member.MembershipStatus} — CK_Member_MembershipStatus.
 * "Expired" is not stored; it is derived from ExpiryDate (R26).
 *
 * <p>Constant names are the stored values, so the column is mapped with
 * {@code @Enumerated(EnumType.STRING)}.
 */
public enum MembershipStatus {
    Active, Suspended, Cancelled
}
