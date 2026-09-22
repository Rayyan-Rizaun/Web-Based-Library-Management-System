package com.lms.common.domain;

/**
 * {@code AppUser.Status} — CK_AppUser_Status.
 *
 * <p>Constant names are the stored values, so the column is mapped with
 * {@code @Enumerated(EnumType.STRING)}.
 */
public enum AppUserStatus {
    Active, Deactivated, Locked
}
