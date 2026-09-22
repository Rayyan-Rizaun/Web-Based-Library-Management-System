package com.lms.common.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * One-off developer utility. Not a Spring bean and not called by the
 * running application — it exists purely so the group can generate a
 * BCrypt hash for a password everyone knows, to drop into the seeded
 * {@code AppUser} rows for demos and the viva (real login is UC-01, not
 * yet built; see {@link SecurityConfig}).
 *
 * <p>BCrypt embeds a random salt in every hash it produces, so running
 * this twice prints two different-looking strings for the same password —
 * that is correct, expected BCrypt behaviour, not a bug. Either hash
 * verifies the password once real authentication checks it with
 * {@link BCryptPasswordEncoder#matches}.
 *
 * <p>Run it from the IDE (right-click the class, Run), or from a terminal
 * with the Spring Boot Maven plugin pointed at this class instead of the
 * application's own main class, so it reuses the project's dependencies
 * without starting the web server:
 *
 * <pre>
 *   mvnw.cmd spring-boot:run "-Dspring-boot.run.main-class=com.lms.common.security.PasswordHashGenerator"
 * </pre>
 */
public final class PasswordHashGenerator {

    /** The password every seeded demo account should share. */
    private static final String DEMO_PASSWORD = "Library@2026";

    private PasswordHashGenerator() {
    }

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(DEMO_PASSWORD);

        System.out.println("Password : " + DEMO_PASSWORD);
        System.out.println("BCrypt   : " + hash);
        System.out.println();
        System.out.println("UPDATE dbo.AppUser SET PasswordHash = N'" + hash + "';");
    }
}
