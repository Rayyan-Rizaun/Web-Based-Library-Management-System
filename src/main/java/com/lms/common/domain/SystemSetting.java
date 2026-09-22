package com.lms.common.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table {@code SystemSetting} — entity SYSTEM_SETTING. The only place policy
 * numbers live — loan period, fine rate, borrowing limits and so on
 * (CLAUDE.md rule 9, business-rules §7). Read a value by key with
 * {@code SystemSettingRepository.findById("Loan.PeriodDays")}.
 */
@Entity
@Table(name = "SystemSetting")
@Getter
@Setter
@NoArgsConstructor
public class SystemSetting {

    /** Natural key, e.g. "Loan.PeriodDays" or "Borrowing.Limit.Academic Staff". Not generated. */
    @Id
    @Column(name = "SettingKey", length = 100)
    private String settingKey;

    /** Stored as text; CK_SystemSetting_ValueMatchesType checks it parses as {@link #settingDataType}. */
    @Column(name = "SettingValue", nullable = false, length = 255)
    private String settingValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "SettingDataType", nullable = false, length = 10)
    private SettingDataType settingDataType;

    @Column(name = "Description", length = 500)
    private String description;

    /** NULL for values seeded by 01_schema.sql. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UpdatedByUserID")
    private AppUser updatedBy;

    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = DbTime.now();
    }
}
