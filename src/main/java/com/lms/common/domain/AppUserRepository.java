package com.lms.common.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Aggregate root {@link AppUser}, including its {@link UserRole} grants.
 * UC-01 (manage accounts and roles), login, and the "email already exists" check.
 */
public interface AppUserRepository extends JpaRepository<AppUser, Integer> {

    /** Login, and the UC-01 duplicate check. Email is the alternate key. */
    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<AppUser> findByStatusOrderByLastNameAscFirstNameAsc(AppUserStatus status);

    /** UC-01 user list search box. */
    Page<AppUser> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String firstName, String lastName, String email, Pageable pageable);

    default Page<AppUser> search(String term, Pageable pageable) {
        return findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                term, term, term, pageable);
    }

    /** Everyone currently holding a role, e.g. "Librarian". */
    List<AppUser> findDistinctByRoleAssignmentsRoleRoleName(String roleName);

    /** Permission check for one user. */
    boolean existsByUserIdAndRoleAssignmentsRoleRoleName(Integer userId, String roleName);

    /** Bootstrap check: does anyone hold this role yet (e.g. "Library Administrator")? */
    boolean existsByRoleAssignmentsRoleRoleName(String roleName);
}
