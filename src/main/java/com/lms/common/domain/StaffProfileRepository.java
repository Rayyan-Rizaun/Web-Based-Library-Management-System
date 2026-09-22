package com.lms.common.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Aggregate root {@link StaffProfile} — the STAFF_PROFILE subtype, reached from an AppUser by UserID.
 */
public interface StaffProfileRepository extends JpaRepository<StaffProfile, Integer> {

    /** The staff profile for a signed-in user, if they have one (the ISA is partial). */
    Optional<StaffProfile> findByUserUserId(Integer userId);

    boolean existsByUserUserId(Integer userId);

    boolean existsByEmployeeNo(String employeeNo);

    List<StaffProfile> findByActiveTrueOrderByEmployeeNo();
}
