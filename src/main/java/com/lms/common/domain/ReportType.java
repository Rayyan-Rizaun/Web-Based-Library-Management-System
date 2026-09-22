package com.lms.common.domain;

/**
 * {@code ReportAudit.ReportType} — CK_ReportAudit_ReportType.
 *
 * <p>Constant names are the stored values, so the column is mapped with
 * {@code @Enumerated(EnumType.STRING)}.
 */
public enum ReportType {
    BookInventory, BorrowedBooks, ReturnedBooks, OverdueBooks, Reservations,
    MemberRegistration, MemberActivity, MostBorrowedBooks, LostDamagedBooks,
    FineCollection, OutstandingFines, FinePaymentHistory, WaivedFines, Revenue,
    Reviews, Feedback
}
