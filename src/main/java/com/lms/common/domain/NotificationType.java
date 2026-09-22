package com.lms.common.domain;

/**
 * {@code Notification.NotificationType} — CK_Notification_NotificationType.
 *
 * <p>Constant names are the stored values, so the column is mapped with
 * {@code @Enumerated(EnumType.STRING)}.
 */
public enum NotificationType {
    ReservationReady, DueSoon, Overdue, FineIssued, AppealDecision,
    RenewalDecision, ReviewDecision, FeedbackResponse, General
}
