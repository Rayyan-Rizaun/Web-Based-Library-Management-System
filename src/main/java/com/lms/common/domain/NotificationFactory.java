package com.lms.common.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class NotificationFactory {

    public Notification appealApproved(AppUser recipient, String fineTypeName, BigDecimal reduction, BigDecimal newBalance) {
        return build(recipient, NotificationType.AppealDecision, "Fine appeal approved",
                "Your appeal for the " + fineTypeName + " fine was approved. A reduction of LKR "
                        + reduction + " was applied — this fine now stands at LKR " + newBalance + ".");
    }

    public Notification appealRejected(AppUser recipient, String fineTypeName, BigDecimal balance) {
        return build(recipient, NotificationType.AppealDecision, "Fine appeal rejected",
                "Your appeal for the " + fineTypeName + " fine was rejected. It remains payable in full at LKR " + balance + ".");
    }

    public Notification reservationReady(AppUser recipient, String bookTitle, LocalDateTime expiresAt) {
        return build(recipient, NotificationType.ReservationReady, "Reservation ready for collection",
                "\"" + bookTitle + "\" is being held for you until " + expiresAt.toLocalDate() + ". Collect it at the front desk.");
    }

    public Notification reservationCancelledByStaff(AppUser recipient, String bookTitle, String reason) {
        return build(recipient, NotificationType.General, "Reservation cancelled",
                "Your reservation for \"" + bookTitle + "\" was cancelled by library staff: " + reason);
    }

    public Notification membershipApproved(AppUser recipient, LocalDate expiryDate) {
        return build(recipient, NotificationType.General, "Membership approved",
                "Your library membership is now active and you can borrow books. It is valid until " + expiryDate + ".");
    }

    public Notification membershipRejected(AppUser recipient, String reason) {
        return build(recipient, NotificationType.General, "Membership registration rejected",
                "Your library membership registration was not approved: " + reason);
    }

    public Notification membershipSuspended(AppUser recipient, String reason) {
        return build(recipient, NotificationType.General, "Membership suspended",
                "Your library membership has been suspended by library staff: " + reason);
    }

    public Notification membershipReactivated(AppUser recipient) {
        return build(recipient, NotificationType.General, "Membership reactivated",
                "Your library membership has been reactivated. You can borrow books again.");
    }

    private Notification build(AppUser recipient, NotificationType type, String title, String message) {
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        return notification;
    }
}
