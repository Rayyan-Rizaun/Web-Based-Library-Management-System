package com.lms.common.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Aggregate root {@link Member} — the MEMBER subtype, reached from an AppUser by UserID.
 * Academic staff are Members with {@code MemberType.AcademicStaff}; there is no separate repository.
 */
public interface MemberRepository extends JpaRepository<Member, Integer> {

    /** The membership for a signed-in user, if they have one (the ISA is partial). */
    Optional<Member> findByUserUserId(Integer userId);

    boolean existsByUserUserId(Integer userId);

    Optional<Member> findByMembershipNo(String membershipNo);

    boolean existsByMembershipNo(String membershipNo);

    boolean existsByNationalId(String nationalId);

    List<Member> findByMemberType(MemberType memberType);

    /** UC-03 "searches for and selects the Library Member". */
    Page<Member> findByUserFirstNameContainingIgnoreCaseOrUserLastNameContainingIgnoreCaseOrMembershipNoContainingIgnoreCase(
            String firstName, String lastName, String membershipNo, Pageable pageable);

    default Page<Member> search(String term, Pageable pageable) {
        return findByUserFirstNameContainingIgnoreCaseOrUserLastNameContainingIgnoreCaseOrMembershipNoContainingIgnoreCase(
                term, term, term, pageable);
    }

    /** Memberships that have expired (R26: expiry is derived from the date, not stored as a status). */
    List<Member> findByExpiryDateBeforeOrderByExpiryDateAsc(LocalDate date);

    /** UC-01 member approval: every Suspended member (pending self-registrations and staff-suspended members alike — {@code MemberAdminService} tells them apart) and every Cancelled (rejected) one, oldest first. */
    List<Member> findByMembershipStatusOrderByJoinedDateAsc(MembershipStatus status);

    /**
     * UC-01 "All Members": one search box against name/membership no./email,
     * an optional status filter, an optional member-type filter, all
     * applied together with paging and sorting. Mirrors {@code
     * FineRepository.searchOutstanding}'s exact "optional {@code :param IS
     * NULL OR ...}" idiom. {@code com.lms.user.MemberAdminService} builds
     * every argument, so this stays a plain query, not a place business
     * rules live (CLAUDE.md rule 5).
     */
    @Query(value = """
            SELECT m FROM Member m
            JOIN m.user u
            WHERE (:statusFilter IS NULL OR m.membershipStatus = :statusFilter)
              AND (:memberTypeFilter IS NULL OR m.memberType = :memberTypeFilter)
              AND (:likeQuery IS NULL
                   OR LOWER(u.firstName) LIKE :likeQuery
                   OR LOWER(u.lastName) LIKE :likeQuery
                   OR LOWER(m.membershipNo) LIKE :likeQuery
                   OR LOWER(u.email) LIKE :likeQuery)
            """,
            countQuery = """
            SELECT COUNT(m) FROM Member m
            JOIN m.user u
            WHERE (:statusFilter IS NULL OR m.membershipStatus = :statusFilter)
              AND (:memberTypeFilter IS NULL OR m.memberType = :memberTypeFilter)
              AND (:likeQuery IS NULL
                   OR LOWER(u.firstName) LIKE :likeQuery
                   OR LOWER(u.lastName) LIKE :likeQuery
                   OR LOWER(m.membershipNo) LIKE :likeQuery
                   OR LOWER(u.email) LIKE :likeQuery)
            """)
    Page<Member> searchAll(@Param("statusFilter") MembershipStatus statusFilter,
                            @Param("memberTypeFilter") MemberType memberTypeFilter,
                            @Param("likeQuery") String likeQuery,
                            Pageable pageable);

    /** UC-08 member registration report. */
    List<Member> findByJoinedDateBetweenOrderByJoinedDateAsc(LocalDate from, LocalDate to);

    @Query(value = """
            SELECT m.MemberType AS MemberType,
                   SUM(CASE WHEN m.JoinedDate BETWEEN :from AND :to THEN 1 ELSE 0 END) AS Registrations,
                   SUM(CASE WHEN m.MembershipStatus = N'Active' THEN 1 ELSE 0 END) AS ActiveCount,
                   SUM(CASE WHEN m.MembershipStatus = N'Suspended' THEN 1 ELSE 0 END) AS SuspendedCount
            FROM Member m
            WHERE (:memberType IS NULL OR m.MemberType = :memberType)
            GROUP BY m.MemberType
            """, nativeQuery = true)
    List<Object[]> usersByMemberType(@Param("from") LocalDate from, @Param("to") LocalDate to,
            @Param("memberType") String memberType);
}
