package com.lms.common.domain;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table {@code AppUser} — entity APP_USER, the supertype of the ISA
 * APP_USER → {STAFF_PROFILE, MEMBER} (00_relational_mapping.md §1.1).
 *
 * <p>Strategy A with separate keys: {@link StaffProfile} and {@link Member}
 * are separate entities that each point here through their own
 * {@code @OneToOne} on UserID. There is no {@code @Inheritance}.
 *
 * <p>This side deliberately has no {@code staffProfile} / {@code member}
 * fields. The inverse side of a {@code @OneToOne} cannot be lazy-loaded, so
 * mapping it would add two extra queries to every AppUser load — including
 * every login. Use {@code StaffProfileRepository.findByUserUserId} and
 * {@code MemberRepository.findByUserUserId} instead.
 *
 * <p>Composite attributes Name and Address are flattened to their
 * components (R2, R3).
 */
@Entity
@Table(name = "AppUser")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    @Setter(AccessLevel.NONE)
    private Integer userId;

    @Column(name = "Email", nullable = false, length = 254)
    private String email;

    /** BCrypt string with the salt embedded; there is no salt column (R25). */
    @Column(name = "PasswordHash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "FirstName", nullable = false, length = 100)
    private String firstName;

    @Column(name = "LastName", nullable = false, length = 100)
    private String lastName;

    @Column(name = "Phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "AddressLine1", length = 150)
    private String addressLine1;

    @Column(name = "AddressLine2", length = 150)
    private String addressLine2;

    @Column(name = "City", length = 100)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private AppUserStatus status = AppUserStatus.Active;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Role grants — table UserRole, part of this aggregate. Adding a
     * {@link UserRole} grants a role; removing one revokes it.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private Set<UserRole> roleAssignments = new HashSet<>();

    @PrePersist
    void onCreate() {
        LocalDateTime now = DbTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = DbTime.now();
    }
}
