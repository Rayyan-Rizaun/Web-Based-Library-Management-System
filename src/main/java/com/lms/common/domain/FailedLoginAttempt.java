package com.lms.common.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table {@code FailedLoginAttempt} — entity FAILED_LOGIN_ATTEMPT.
 *
 * <p>Deliberately no association to {@link AppUser}: the email tried may not
 * belong to any account, and those attempts must still be recorded. The
 * schema has no foreign key here for the same reason.
 */
@Entity
@Table(name = "FailedLoginAttempt")
@Getter
@Setter
@NoArgsConstructor
public class FailedLoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AttemptID")
    @Setter(AccessLevel.NONE)
    private Long attemptId;

    @Column(name = "EmailTried", nullable = false, length = 254)
    private String emailTried;

    /** 45 characters fits an IPv6 address. */
    @Column(name = "IPAddress", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "UserAgent", length = 500)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "FailureReason", nullable = false, length = 20)
    private FailureReason failureReason;

    @Column(name = "AttemptedAt", nullable = false, updatable = false)
    private LocalDateTime attemptedAt;

    @PrePersist
    void onCreate() {
        if (attemptedAt == null) {
            attemptedAt = DbTime.now();
        }
    }
}
