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

/** Table {@code AuditLog} — entity AUDIT_LOG (1:N PERFORMED BY). */
@Entity
@Table(name = "AuditLog")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    /** BIGINT — the audit log is the fastest-growing table. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AuditID")
    @Setter(AccessLevel.NONE)
    private Long auditId;

    /** NULL for system or trigger actions. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID")
    private AppUser user;

    @Column(name = "ActionName", nullable = false, length = 50)
    private String actionName;

    @Column(name = "EntityName", nullable = false, length = 50)
    private String entityName;

    /** Text, because SystemSetting's key is text. */
    @Column(name = "EntityID", length = 100)
    private String entityId;

    /** JSON text, NVARCHAR(MAX); CK_AuditLog_DetailsIsJson rejects invalid JSON. */
    @Column(name = "Details")
    private String details;

    @Column(name = "OccurredAt", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    void onCreate() {
        if (occurredAt == null) {
            occurredAt = DbTime.now();
        }
    }
}
