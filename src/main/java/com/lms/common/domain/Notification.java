package com.lms.common.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Table {@code Notification} — entity NOTIFICATION (1:N RECEIVES). */
@Entity
@Table(name = "Notification")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotificationID")
    @Setter(AccessLevel.NONE)
    private Integer notificationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserID", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "NotificationType", nullable = false, length = 30)
    private NotificationType notificationType;

    @Column(name = "Title", nullable = false, length = 150)
    private String title;

    @Column(name = "Message", nullable = false, length = 1000)
    private String message;

    @Column(name = "IsRead", nullable = false)
    private boolean read = false;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = DbTime.now();
        }
    }
}
