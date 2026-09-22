package com.lms.common.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table {@code UserRole} — the M:N relationship ASSIGNS ROLE (R4). Part of
 * the {@link AppUser} aggregate: create one and add it to
 * {@code user.getRoleAssignments()}, then save the user.
 *
 * <p>An entity rather than a plain join table because the relationship
 * carries its own attributes: when the role was granted and by whom.
 */
@Entity
@Table(name = "UserRole")
@Getter
@Setter
@NoArgsConstructor
public class UserRole {

    @EmbeddedId
    @Setter(AccessLevel.NONE)
    private UserRoleId id = new UserRoleId();

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserID")
    private AppUser user;

    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RoleID")
    private Role role;

    @Column(name = "AssignedAt", nullable = false)
    private LocalDateTime assignedAt;

    /**
     * Who granted the role. NULL only for the bootstrap Library Administrator
     * grant — see the BOOTSTRAP DECISION comment in 01_schema.sql.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AssignedByUserID")
    private AppUser assignedBy;

    public UserRole(AppUser user, Role role, AppUser assignedBy) {
        this.user = user;
        this.role = role;
        this.assignedBy = assignedBy;
    }

    @PrePersist
    void onCreate() {
        if (assignedAt == null) {
            assignedAt = DbTime.now();
        }
    }
}
