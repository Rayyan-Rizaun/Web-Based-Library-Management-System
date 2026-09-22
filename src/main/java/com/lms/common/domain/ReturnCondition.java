package com.lms.common.domain;

/**
 * {@code Loan.ReturnCondition} — CK_Loan_ReturnCondition.
 *
 * <p>Constant names are the stored values, so the column is mapped with
 * {@code @Enumerated(EnumType.STRING)}.
 */
public enum ReturnCondition {
    Good, Fair, Poor, Damaged
}
