package com.lms.common.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Table {@code PasswordResetToken} — not part of the original EER, added
 * directly to {@code 01_schema.sql} for UC-01's email-token password reset
 * (see that file's comment on the table, and business-rules.md §8).
 *
 * <p>Only a SHA-256 hash of the token is stored ({@link #tokenHash}), never
 * the token itself — same reasoning as {@code AppUser.PasswordHash} (R25):
 * a database read must not be enough to produce a usable reset link.
 * Hashing happens in {@code com.lms.user.PasswordResetService}, not here.
 */
@Entity
@Table(name = "PasswordResetToken")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TokenID")
    @Setter(AccessLevel.NONE)
    private Integer tokenId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserID", nullable = false)
    private AppUser user;

    /** SHA-256, hex-encoded, 64 characters — CK_PasswordResetToken_TokenHashFormat. */
    @Column(name = "TokenHash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "ExpiresAt", nullable = false)
    private LocalDateTime expiresAt;

    /** NULL until the token is used to set a new password. */
    @Column(name = "ConsumedAt")
    private LocalDateTime consumedAt;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "RequestIP", length = 45)
    private String requestIp;

    public PasswordResetToken(AppUser user, String tokenHash, LocalDateTime expiresAt, String requestIp) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.requestIp = requestIp;
    }

    /** True while this token can still be exchanged for a new password. */
    public boolean isUsable(LocalDateTime now) {
        return consumedAt == null && now.isBefore(expiresAt);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = DbTime.now();
        }
    }
}
