package com.lms.common.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Aggregate root {@link PasswordResetToken}. UC-01 forgot-password /
 * reset-password flow.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {

    /** The live-lookup index (IX_PasswordResetToken_Lookup) is filtered the same way. */
    Optional<PasswordResetToken> findByTokenHashAndConsumedAtIsNull(String tokenHash);
}
