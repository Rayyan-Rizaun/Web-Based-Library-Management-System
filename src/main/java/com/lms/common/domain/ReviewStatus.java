package com.lms.common.domain;

/**
 * {@code BookReview.Status} — CK_BookReview_Status. Also used for
 * {@code ReviewModerationHistory.PreviousStatus / NewStatus}, whose checks share this domain.
 *
 * <p>Constant names are the stored values, so the column is mapped with
 * {@code @Enumerated(EnumType.STRING)}.
 */
public enum ReviewStatus {
    Pending, Approved, Rejected, Hidden, Removed
}
