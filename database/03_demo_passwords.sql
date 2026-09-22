/* =============================================================================
   Library Management System — demo login passwords
   IT2140 Part 02 · Group MLB-B11G2-10 · Microsoft SQL Server 2022
   -----------------------------------------------------------------------------
   Run after database/02_seed_01_identity.sql. Sets every seeded AppUser's
   password to:  Library@2026
   so the group can sign in as any seeded account during a demo or the viva
   (once UC-01 login exists).

   The hash was produced by com.lms.common.security.PasswordHashGenerator
   (BCryptPasswordEncoder, strength 10, spring-security-crypto 6.5.11 — the
   version this project's Spring Boot 3.5.16 parent manages). Re-running that
   class prints a different-looking hash each time, because BCrypt embeds a
   random salt; any of them verifies the same password.

   DEMO DATA ONLY. Every account sharing one known password is fine for
   seeded test accounts and never acceptable for real users.
   ============================================================================= */

SET NOCOUNT ON;
GO

UPDATE dbo.AppUser
SET    PasswordHash = N'$2a$10$BlFmJFDbLj8X81A3WYrA0uIqFZDsQH0LVfJkOfiEyzlGg54LYHyaS',
       UpdatedAt    = SYSDATETIME();   -- keeps CK_AppUser_UpdatedAfterCreated true
GO
