package com.lms.common.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Aggregate root {@link Role}. UC-01: the role picker when assigning roles.
 */
public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByRoleName(String roleName);

    List<Role> findByActiveTrueOrderByRoleName();
}
