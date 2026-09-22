package com.lms.common.domain;

/**
 * {@code Fine.FineType} — CK_Fine_FineType.
 *
 * <p>Constant names are the stored values, so the column is mapped with
 * {@code @Enumerated(EnumType.STRING)}.
 */
public enum FineType {
    Overdue, Lost, Damaged
}
