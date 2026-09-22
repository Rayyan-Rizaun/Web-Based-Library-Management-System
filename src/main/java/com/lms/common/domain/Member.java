package com.lms.common.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table {@code Member} — subtype MEMBER of APP_USER
 * (00_relational_mapping.md §1.1, Strategy A), and the single table for the
 * nested ISA MEMBER → ACADEMIC_STAFF (§1.2, Strategy C).
 *
 * <p>Has its OWN primary key, MemberID, linked to the supertype through the
 * UNIQUE NOT NULL foreign key UserID. The ISA is overlapping: the same
 * AppUser may also have a {@link StaffProfile}.
 *
 * <p>ACADEMIC_STAFF is not a table or a class. An academic staff member is
 * a Member with {@code memberType == MemberType.AcademicStaff}. The borrowing
 * limit is not stored here (R10): it is the SystemSetting row
 * {@code "Borrowing.Limit." + memberType.dbValue()}.
 */
@Entity
@Table(name = "Member")
@Getter
@Setter
@NoArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MemberID")
    @Setter(AccessLevel.NONE)
    private Integer memberId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserID", nullable = false, unique = true)
    private AppUser user;

    @Column(name = "MembershipNo", nullable = false, length = 20)
    private String membershipNo;

    /** Optional; unique when present (filtered index UX_Member_NationalID). */
    @Column(name = "NationalID", length = 20)
    private String nationalId;

    /** The Strategy C discriminator. */
    @Convert(converter = MemberType.JpaConverter.class)
    @Column(name = "MemberType", nullable = false, length = 20)
    private MemberType memberType = MemberType.Student;

    @Column(name = "JoinedDate", nullable = false)
    private LocalDate joinedDate;

    @Column(name = "ExpiryDate", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "MembershipStatus", nullable = false, length = 20)
    private MembershipStatus membershipStatus = MembershipStatus.Active;

    @PrePersist
    void onCreate() {
        if (joinedDate == null) {
            joinedDate = LocalDate.now();
        }
    }
}
