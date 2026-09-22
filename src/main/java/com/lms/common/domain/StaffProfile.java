package com.lms.common.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table {@code StaffProfile} — subtype STAFF_PROFILE of APP_USER
 * (00_relational_mapping.md §1.1, Strategy A).
 *
 * <p>Has its OWN primary key, StaffID. The link to the supertype is the
 * UNIQUE NOT NULL foreign key UserID, mapped as the owning side of a
 * {@code @OneToOne}. The ISA is overlapping: the same AppUser may also have
 * a {@link Member} row.
 *
 * <p>Staff-action foreign keys elsewhere (who issued a loan, who decided an
 * appeal) point at this entity, not at AppUser (R27).
 */
@Entity
@Table(name = "StaffProfile")
@Getter
@Setter
@NoArgsConstructor
public class StaffProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StaffID")
    @Setter(AccessLevel.NONE)
    private Integer staffId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserID", nullable = false, unique = true)
    private AppUser user;

    @Column(name = "EmployeeNo", nullable = false, length = 20)
    private String employeeNo;

    @Column(name = "JobTitle", nullable = false, length = 100)
    private String jobTitle;

    @Column(name = "JoinedDate", nullable = false)
    private LocalDate joinedDate;

    /** Employment status — separate from {@code AppUser.status}, which controls login. */
    @Column(name = "IsActive", nullable = false)
    private boolean active = true;
}
