package com.lms.common.domain;

/**
 * {@code ReviewFlag.Status} — CK_ReviewFlag_Status.
 *
 * <p>Constant names are the stored values, so the column is mapped with
 * {@code @Enumerated(EnumType.STRING)}.
 */
public enum FlagStatus {
    Open, Upheld, Dismissed
}
