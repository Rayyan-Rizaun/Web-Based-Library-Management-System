/* =============================================================================
   Library Management System — Seed data, Part 1: Identity and Access
   IT2140 Part 02 · Group MLB-B11G2-10 · Microsoft SQL Server 2022
   -----------------------------------------------------------------------------
   Run immediately after database/01_schema.sql on a fresh database.
   Seeds, in FK-safe order: Role, AppUser, UserRole, StaffProfile, Member.

   Identity values (UserID 1-6, RoleID 1-6, StaffID 1-6, MemberID 1-6) are
   deterministic because this runs on a freshly created schema where every
   IDENTITY column starts at 1 — later seed files (02_seed_02_*, ...) rely on
   these exact numbers.

   Coverage required by this file alone:
     - 1 Library Administrator                (UserID 1)
     - 2 Librarians                            (UserID 2, 3)
     - 1 Finance Officer                       (UserID 4)
     - 1 person who is both staff AND member   (UserID 5 — Librarian + Student member;
                                                  in fact every seeded user here is both,
                                                  since only 6 AppUser rows exist and
                                                  both StaffProfile and Member need 6 rows —
                                                  this is the overlapping ISA (R30) in action)
     - 2 Academic Staff members                (MemberID 1, 2)
     - 1 expired membership                    (MemberID 6 — ExpiryDate before "today")
   ============================================================================= */

SET NOCOUNT ON;
GO

/* -----------------------------------------------------------------------------
   1. ROLE  (6 rows) — the four operational roles from Requirements §3, plus
      Library Member (held by every borrowing account) and Guest Visitor
      (kept in the table for completeness; no AppUser row uses it, since a
      guest has no login per business-rules §3).
   ----------------------------------------------------------------------------- */
INSERT INTO dbo.Role (RoleName, RoleDescription, IsActive) VALUES
    (N'Library Administrator', N'Full system access: manages users, roles, reports, fine waivers and system settings.', 1),
    (N'Librarian',             N'Manages the catalogue, members, borrowing, returns, reservations and fine recording.', 1),
    (N'Finance Officer',       N'Views and manages fine payments, receipts and financial reports.', 1),
    (N'Library Member',        N'Searches the catalogue, borrows books, reserves titles and views their own account.', 1),
    (N'Academic Staff Member', N'A Library Member with a higher borrowing limit and priority reservation handling.', 1),
    (N'Guest Visitor',         N'Unauthenticated browsing of the catalogue and published reviews; cannot borrow or reserve.', 0);
GO

/* -----------------------------------------------------------------------------
   2. APP_USER  (6 rows)
   ----------------------------------------------------------------------------- */
INSERT INTO dbo.AppUser (Email, PasswordHash, FirstName, LastName, Phone, AddressLine1, AddressLine2, City) VALUES
    (N'nuwan.perera@nlms.lk',       N'$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', N'Nuwan',   N'Perera',      N'+94 71 234 5601', N'45 Galle Road',        NULL,               N'Colombo 03'),
    (N'ama.silva@nlms.lk',          N'$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', N'Ama',     N'Silva',       N'+94 71 234 5602', N'12 Kandy Road',        N'Peradeniya',      N'Kandy'),
    (N'kasun.fernando@nlms.lk',     N'$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', N'Kasun',   N'Fernando',    N'+94 71 234 5603', N'78 Main Street',       NULL,               N'Negombo'),
    (N'chamari.wijesinghe@nlms.lk', N'$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', N'Chamari', N'Wijesinghe',  N'+94 71 234 5604', N'23 Havelock Road',     NULL,               N'Colombo 05'),
    (N'dilani.rathnayake@nlms.lk',  N'$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', N'Dilani',  N'Rathnayake',  N'+94 71 234 5605', N'156 Temple Road',      N'Nugegoda',        N'Colombo'),
    (N'sanduni.jayawardena@nlms.lk',N'$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', N'Sanduni', N'Jayawardena', N'+94 71 234 5606', N'9 Lake Drive',         NULL,               N'Kurunegala');
GO

/* -----------------------------------------------------------------------------
   3. USER_ROLE  (6 rows) — bootstrap: UserID 1 grants their own Library
      Administrator role with AssignedByUserID = NULL, since no administrator
      exists yet to grant it (the bootstrap decision recorded in 01_schema.sql).
      Every subsequent grant is made by UserID 1.
   ----------------------------------------------------------------------------- */
INSERT INTO dbo.UserRole (UserID, RoleID, AssignedByUserID) VALUES
    (1, 1, NULL),   -- Nuwan Perera    -> Library Administrator (bootstrap grant)
    (2, 2, 1),      -- Ama Silva       -> Librarian
    (3, 2, 1),      -- Kasun Fernando  -> Librarian
    (4, 3, 1),      -- Chamari Wijesinghe -> Finance Officer
    (5, 2, 1),      -- Dilani Rathnayake  -> Librarian
    (6, 2, 1);      -- Sanduni Jayawardena -> Librarian
GO

/* -----------------------------------------------------------------------------
   4. STAFF_PROFILE  (6 rows) — every seeded user holds a staff position.
   ----------------------------------------------------------------------------- */
INSERT INTO dbo.StaffProfile (UserID, EmployeeNo, JobTitle, JoinedDate, IsActive) VALUES
    (1, N'E001', N'Library Director',  '2018-03-01', 1),
    (2, N'E002', N'Senior Librarian',  '2019-06-15', 1),
    (3, N'E003', N'Librarian',         '2021-01-10', 1),
    (4, N'E004', N'Finance Officer',   '2020-09-01', 1),
    (5, N'E005', N'Librarian',         '2022-02-20', 1),
    (6, N'E006', N'Library Assistant', '2023-05-05', 1);
GO

/* -----------------------------------------------------------------------------
   5. MEMBER  (6 rows) — every seeded staff member also holds a borrowing
      membership (the overlapping ISA, R30). MemberID 1 and 2 are Academic
      Staff type; MemberID 6 (Sanduni) has an ExpiryDate before "today", so
      her membership reads as expired via the derived rule (R26) even though
      MembershipStatus itself stays 'Active'.
   ----------------------------------------------------------------------------- */
INSERT INTO dbo.Member (UserID, MembershipNo, NationalID, MemberType, JoinedDate, ExpiryDate, MembershipStatus) VALUES
    (1, N'MEM-2025-001', N'196534501234', N'Academic Staff', '2025-10-01', '2026-10-01', N'Active'),   -- Nuwan Perera
    (2, N'MEM-2025-002', N'198765401234', N'Academic Staff', '2025-11-01', '2026-11-01', N'Active'),   -- Ama Silva
    (3, N'MEM-2026-003', NULL,            N'Student',        '2026-01-15', '2027-01-15', N'Active'),   -- Kasun Fernando
    (4, N'MEM-2026-004', N'199012345678', N'Student',        '2026-02-01', '2027-02-01', N'Active'),   -- Chamari Wijesinghe
    (5, N'MEM-2026-005', NULL,            N'Student',        '2026-03-01', '2027-03-01', N'Active'),   -- Dilani Rathnayake (staff + member showcase)
    (6, N'MEM-2025-006', N'200145678901', N'Student',        '2025-06-01', '2026-06-01', N'Active');   -- Sanduni Jayawardena -- EXPIRED
GO
