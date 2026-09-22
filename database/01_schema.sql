CREATE DATABASE LibraryDB;
GO
USE LibraryDB;
GO
/* =============================================================================
   Library Management System — Schema (D2)
   IT2140 Part 02 · Group MLB-B11G2-10 · Microsoft SQL Server 2022
   -----------------------------------------------------------------------------
   Design source : database/00_relational_mapping.md (EER -> relational mapping
                   and the numbered refinements referenced below as "R#").
   Values        : docs/business-rules.md (seeded into SystemSetting at the end).

   HOW TO RUN
   - Run inside the target database (not master). The script drops and
     recreates every table, so re-running it wipes all data. That is the
     intended workflow in CLAUDE.md: edit this file, then re-run.
   - Filtered unique indexes are used for "at most one active X" rules.
     Every session that inserts or updates these tables needs
     QUOTED_IDENTIFIER ON and ANSI_NULLS ON. SSMS, Azure Data Studio and the
     Microsoft JDBC driver set both by default; sqlcmd does not, so run it
     with `sqlcmd -I`.

   CONVENTIONS
   - Object names are PascalCase (CLAUDE.md rule 8). Every constraint is
     named with a type prefix:
       PK_   primary key          UQ_  unique constraint (alternate key)
       FK_   foreign key          UX_  filtered unique index
       CK_   check constraint     DF_  default
       IX_   plain index
   - ON UPDATE NO ACTION on every FK. All referenced keys are IDENTITY
     surrogates and never change, so no update ever needs to propagate.
   - ON DELETE NO ACTION on every FK except pure dependants, marked CASCADE.
     Users, books and copies are deactivated, never deleted. The Library
     Director's interview asks that a book no longer held be "marked as
     removed or inactive instead of deleting its history". SQL Server also
     rejects multiple cascade paths into AppUser.
   - Money is DECIMAL(10,2) and timestamps are DATETIME2(0). Calendar-only
     values are DATE.
   - Rules that must read other rows (borrowing limit, max 2 renewals, "must
     have borrowed before reviewing", fine status upkeep) cannot be CHECK
     constraints. They are D5's stored procedures and triggers, noted inline
     as [D5].

   TABLE  <-  EER SOURCE
   ---------------------------------------------------------------------------
   Identity and access
     Role                     <- entity ROLE
     AppUser                  <- entity APP_USER (ISA supertype); composite
                                 Name and Address flattened (R2, R3)
     StaffProfile             <- entity STAFF_PROFILE (subtype; ISA mapped
                                 table-per-entity, overlapping — R30)
     Member                   <- entity MEMBER (subtype); also holds the
                                 ACADEMIC_STAFF subtype via the MemberType
                                 discriminator (single-table, R11)
     UserRole                 <- M:N relationship ASSIGNS ROLE (R4)
     PasswordResetToken       <- not in the EER; UC-01 email-token password
                                 reset mechanism, added directly to the DDL
   Catalog and inventory
     Publisher                <- entity PUBLISHER
     Author                   <- entity AUTHOR
     Category                 <- entity CATEGORY
     Book                     <- entity BOOK; PublisherID from 1:N PUBLISHED BY (R9)
     BookAuthor               <- M:N relationship WRITES, with AuthorOrder (R5)
     BookCategory             <- M:N relationship CLASSIFIES (R6)
     BookKeyword              <- multivalued attribute BOOK.[M] Keywords (R1)
     BookCopy                 <- entity BOOK_COPY; BookID from 1:N COPIES OF
   Circulation
     Loan                     <- aggregation MEMBER—BORROWS—BOOK_COPY (R7);
                                 staff FKs from ISSUED/RETURNED BY (R27)
     LoanRenewal              <- entity LOAN_RENEWAL; LoanID from 1:N RENEWALS
     Reservation              <- entity RESERVATION; 1:N PLACES (Member) and
                                 1:N RESERVES (Book) (R8)
   Fines, appeals and payments
     BookIncident             <- entity BOOK_INCIDENT; 1:N AFFECTS (copy),
                                 ARISES FROM (loan), LIABLE MEMBER
     Fine                     <- entity FINE; 1:N CHARGED TO, OVERDUE SOURCE,
                                 LOSS/DAMAGE SOURCE (R16, R17, R29)
     FineAppeal               <- entity FINE_APPEAL; 1:N APPEALS (R19)
     FinePayment              <- entity FINE_PAYMENT; 1:N PARTIAL/FULL PAYMENTS
   Reviews and feedback
     BookReview               <- entity BOOK_REVIEW; 1:N REVIEWS, WRITES (R22)
     ReviewFlag               <- entity REVIEW_FLAG; 1:N FLAGS, REPORTED BY
     ReviewModerationHistory  <- entity REVIEW_MODERATION_HISTORY; 1:N
                                 MODERATION HISTORY
     FeedbackCategory         <- entity FEEDBACK_CATEGORY
     MemberFeedback           <- entity MEMBER_FEEDBACK; 1:N SUBMITS, BELONGS TO
     FeedbackHistory          <- entity FEEDBACK_HISTORY; 1:N STATUS HISTORY
   Operations and governance
     Notification             <- entity NOTIFICATION; 1:N RECEIVES
     AuditLog                 <- entity AUDIT_LOG; 1:N PERFORMED BY
     ReportAudit              <- entity REPORT_AUDIT; 1:N GENERATED BY
     SystemSetting            <- entity SYSTEM_SETTING; 1:N UPDATED BY
     FailedLoginAttempt       <- entity FAILED_LOGIN_ATTEMPT (no FK on purpose)
     DatabaseBackupLog        <- entity DATABASE_BACKUP_LOG; 1:N INITIATED BY
   (No AcademicStaff table: see Member.)
   ============================================================================= */

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
GO

-- Safety guard: refuse to build the schema inside a system database.
DECLARE @CurrentDb SYSNAME = DB_NAME();
IF @CurrentDb IN (N'master', N'model', N'msdb', N'tempdb')
BEGIN
    RAISERROR(N'01_schema.sql must be run inside the LMS database, not %s.', 16, 1, @CurrentDb);
    SET NOEXEC ON;
END
GO

/* -----------------------------------------------------------------------------
   0. Drop existing tables, children before parents (reverse creation order)
   ----------------------------------------------------------------------------- */
DROP TABLE IF EXISTS dbo.DatabaseBackupLog;
DROP TABLE IF EXISTS dbo.FailedLoginAttempt;
DROP TABLE IF EXISTS dbo.SystemSetting;
DROP TABLE IF EXISTS dbo.ReportAudit;
DROP TABLE IF EXISTS dbo.AuditLog;
DROP TABLE IF EXISTS dbo.Notification;
DROP TABLE IF EXISTS dbo.FeedbackHistory;
DROP TABLE IF EXISTS dbo.MemberFeedback;
DROP TABLE IF EXISTS dbo.FeedbackCategory;
DROP TABLE IF EXISTS dbo.ReviewModerationHistory;
DROP TABLE IF EXISTS dbo.ReviewFlag;
DROP TABLE IF EXISTS dbo.BookReview;
DROP TABLE IF EXISTS dbo.FinePayment;
DROP TABLE IF EXISTS dbo.FineAppeal;
DROP TABLE IF EXISTS dbo.Fine;
DROP TABLE IF EXISTS dbo.BookIncident;
DROP TABLE IF EXISTS dbo.Reservation;
DROP TABLE IF EXISTS dbo.LoanRenewal;
DROP TABLE IF EXISTS dbo.Loan;
DROP TABLE IF EXISTS dbo.BookCopy;
DROP TABLE IF EXISTS dbo.BookKeyword;
DROP TABLE IF EXISTS dbo.BookCategory;
DROP TABLE IF EXISTS dbo.BookAuthor;
DROP TABLE IF EXISTS dbo.Book;
DROP TABLE IF EXISTS dbo.Category;
DROP TABLE IF EXISTS dbo.Author;
DROP TABLE IF EXISTS dbo.Publisher;
DROP TABLE IF EXISTS dbo.PasswordResetToken;
DROP TABLE IF EXISTS dbo.UserRole;
DROP TABLE IF EXISTS dbo.Member;
DROP TABLE IF EXISTS dbo.StaffProfile;
DROP TABLE IF EXISTS dbo.AppUser;
DROP TABLE IF EXISTS dbo.Role;
GO

/* =============================================================================
   1. IDENTITY AND ACCESS
   ============================================================================= */

CREATE TABLE dbo.Role (
    RoleID          INT IDENTITY(1,1) NOT NULL,
    RoleName        NVARCHAR(50)      NOT NULL,
    RoleDescription NVARCHAR(255)     NULL,
    IsActive        BIT               NOT NULL CONSTRAINT DF_Role_IsActive DEFAULT (1),

    CONSTRAINT PK_Role          PRIMARY KEY (RoleID),
    CONSTRAINT UQ_Role_RoleName UNIQUE (RoleName),
    CONSTRAINT CK_Role_RoleName_NotBlank CHECK (LEN(TRIM(RoleName)) > 0)
);
GO

-- ISA supertype. Strategy: table-per-entity (mapping doc §1.1).
CREATE TABLE dbo.AppUser (
    UserID        INT IDENTITY(1,1) NOT NULL,
    Email         NVARCHAR(254)     NOT NULL,
    -- BCrypt string with the salt embedded; no separate salt column (R25).
    -- 100 chars leaves room for Spring's "{bcrypt}" prefix.
    PasswordHash  NVARCHAR(100)     NOT NULL,
    FirstName     NVARCHAR(100)     NOT NULL,   -- composite Name (R2)
    LastName      NVARCHAR(100)     NOT NULL,
    -- Mandatory: the security interview lists "contact number" as a required field.
    Phone         NVARCHAR(20)      NOT NULL,
    AddressLine1  NVARCHAR(150)     NULL,       -- composite Address (R3)
    AddressLine2  NVARCHAR(150)     NULL,
    City          NVARCHAR(100)     NULL,
    Status        NVARCHAR(20)      NOT NULL CONSTRAINT DF_AppUser_Status    DEFAULT (N'Active'),
    CreatedAt     DATETIME2(0)      NOT NULL CONSTRAINT DF_AppUser_CreatedAt DEFAULT (SYSDATETIME()),
    UpdatedAt     DATETIME2(0)      NOT NULL CONSTRAINT DF_AppUser_UpdatedAt DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_AppUser       PRIMARY KEY (UserID),
    CONSTRAINT UQ_AppUser_Email UNIQUE (Email),
    CONSTRAINT CK_AppUser_Status CHECK (Status IN (N'Active', N'Deactivated', N'Locked')),
    CONSTRAINT CK_AppUser_EmailFormat CHECK (Email LIKE N'%_@_%._%' AND Email NOT LIKE N'% %'),
    CONSTRAINT CK_AppUser_PhoneFormat CHECK (Phone NOT LIKE N'%[^0-9+ ()-]%' AND LEN(Phone) >= 7),
    CONSTRAINT CK_AppUser_NameNotBlank CHECK (LEN(TRIM(FirstName)) > 0 AND LEN(TRIM(LastName)) > 0),
    -- The Address components travel together: either no address, or Line1 + City.
    CONSTRAINT CK_AppUser_AddressComplete CHECK (
        (AddressLine1 IS NULL AND AddressLine2 IS NULL AND City IS NULL)
        OR (AddressLine1 IS NOT NULL AND City IS NOT NULL)
    ),
    CONSTRAINT CK_AppUser_UpdatedAfterCreated CHECK (UpdatedAt >= CreatedAt)
);
GO

-- ISA subtype (overlapping with Member, R30). UserID is the 1:1 link to the supertype.
CREATE TABLE dbo.StaffProfile (
    StaffID     INT IDENTITY(1,1) NOT NULL,
    UserID      INT               NOT NULL,
    EmployeeNo  NVARCHAR(20)      NOT NULL,
    JobTitle    NVARCHAR(100)     NOT NULL,
    JoinedDate  DATE              NOT NULL,
    -- Employment status, deliberately separate from AppUser.Status (login):
    -- a former librarian can still log in as a Member.
    IsActive    BIT               NOT NULL CONSTRAINT DF_StaffProfile_IsActive DEFAULT (1),

    CONSTRAINT PK_StaffProfile            PRIMARY KEY (StaffID),
    CONSTRAINT UQ_StaffProfile_UserID     UNIQUE (UserID),
    CONSTRAINT UQ_StaffProfile_EmployeeNo UNIQUE (EmployeeNo),
    CONSTRAINT FK_StaffProfile_AppUser FOREIGN KEY (UserID)
        REFERENCES dbo.AppUser (UserID) ON DELETE NO ACTION ON UPDATE NO ACTION
);
GO

-- ISA subtype of AppUser, and the single table for MEMBER -> ACADEMIC_STAFF.
-- MemberType is the discriminator. One column with one value per row makes
-- that ISA disjoint by construction. BorrowingLimit and
-- CanReserveUnavailableBooks were removed (R10, R11): the limit is read from
-- SystemSetting key 'Borrowing.Limit.' + MemberType.
CREATE TABLE dbo.Member (
    MemberID          INT IDENTITY(1,1) NOT NULL,
    UserID            INT               NOT NULL,
    MembershipNo      NVARCHAR(20)      NOT NULL,
    NationalID        NVARCHAR(20)      NULL,       -- optional; unique when present (UX below)
    MemberType        NVARCHAR(20)      NOT NULL CONSTRAINT DF_Member_MemberType DEFAULT (N'Student'),
    JoinedDate        DATE              NOT NULL CONSTRAINT DF_Member_JoinedDate DEFAULT (CAST(SYSDATETIME() AS DATE)),
    ExpiryDate        DATE              NOT NULL,
    -- 'Expired' is not a stored status; it is derived from ExpiryDate (R26).
    MembershipStatus  NVARCHAR(20)      NOT NULL CONSTRAINT DF_Member_MembershipStatus DEFAULT (N'Active'),

    CONSTRAINT PK_Member              PRIMARY KEY (MemberID),
    CONSTRAINT UQ_Member_UserID       UNIQUE (UserID),
    CONSTRAINT UQ_Member_MembershipNo UNIQUE (MembershipNo),
    CONSTRAINT FK_Member_AppUser FOREIGN KEY (UserID)
        REFERENCES dbo.AppUser (UserID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_Member_MemberType       CHECK (MemberType IN (N'Student', N'Academic Staff')),
    CONSTRAINT CK_Member_MembershipStatus CHECK (MembershipStatus IN (N'Active', N'Suspended', N'Cancelled')),
    CONSTRAINT CK_Member_ExpiryAfterJoined CHECK (ExpiryDate > JoinedDate)
);
GO

-- Unique only when present, so members without a national ID are allowed (same pattern as Book.ISBN10).
CREATE UNIQUE NONCLUSTERED INDEX UX_Member_NationalID
    ON dbo.Member (NationalID) WHERE NationalID IS NOT NULL;
GO

/* M:N ASSIGNS ROLE (R4).

   BOOTSTRAP DECISION: AssignedByUserID is NULLABLE, and NULL means
   "granted by the system, not by a person". It is used for the very first
   Library Administrator, created by the seed script (D3) when no
   administrator exists yet to grant the role.

   Rejected alternative: a seeded "SYSTEM" AppUser account as the grantor.
   - It must fake an Email, PasswordHash and Phone to satisfy AppUser's
     constraints.
   - It would be counted in the "total registered users" dashboard figure
     and the Users report.
   - It is a real login row, and becomes an attack surface if a hash is
     ever set on it.

   With NULL, the FK still guarantees that every non-NULL grantor is a real
   user, and CK_UserRole_NotSelfAssigned still stops a person granting a role
   to themselves. [D5] a trigger should reject NULL once any user holds the
   Library Administrator role, so NULL cannot become an unaudited back door
   after bootstrap. */
CREATE TABLE dbo.UserRole (
    UserID           INT          NOT NULL,
    RoleID           INT          NOT NULL,
    AssignedAt       DATETIME2(0) NOT NULL CONSTRAINT DF_UserRole_AssignedAt DEFAULT (SYSDATETIME()),
    AssignedByUserID INT          NULL,

    CONSTRAINT PK_UserRole PRIMARY KEY (UserID, RoleID),
    -- CASCADE: a grant has no meaning without its user. This is the only
    -- cascading path into AppUser from this table.
    CONSTRAINT FK_UserRole_AppUser FOREIGN KEY (UserID)
        REFERENCES dbo.AppUser (UserID) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT FK_UserRole_Role FOREIGN KEY (RoleID)
        REFERENCES dbo.Role (RoleID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_UserRole_AssignedBy FOREIGN KEY (AssignedByUserID)
        REFERENCES dbo.AppUser (UserID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_UserRole_NotSelfAssigned CHECK (AssignedByUserID IS NULL OR AssignedByUserID <> UserID)
);
GO

/* Added for UC-01 password reset (not in the original ER diagram — email
   token delivery is an implementation mechanism, not a business entity).
   Follows CLAUDE.md rule 1's process: this table is added here first, the
   script re-run, then PasswordResetToken.java mapped to match.

   TokenHash, not the token itself: only a SHA-256 hex digest is stored, so
   a database read (a backup, a compromised admin login) cannot produce a
   usable reset link — the same reasoning R25 already applies to
   AppUser.PasswordHash. The mailed link carries the raw token; the request
   handler hashes whatever it receives and looks up that hash.

   One row per request, not per user: requesting a second reset before using
   the first does not invalidate it here (no UNIQUE on UserID) — the D5/Java
   validity check is "unused AND unexpired AND matches this hash", so an
   old, still-valid email link keeps working even if a newer one was also
   requested. A used or expired row is simply never matched again; nothing
   deletes it, so ConsumedAt / ExpiresAt double as the audit trail of every
   reset attempt. */
CREATE TABLE dbo.PasswordResetToken (
    TokenID     INT IDENTITY(1,1) NOT NULL,
    UserID      INT               NOT NULL,
    -- NVARCHAR, not CHAR, matching every other text column in this schema
    -- (PersistenceConfig maps every Java String to NVARCHAR; a fixed-width
    -- CHAR column here would fail Hibernate's schema validation even
    -- though the value itself is always exactly 64 hex characters).
    TokenHash   NVARCHAR(64)      NOT NULL,   -- SHA-256, hex-encoded
    ExpiresAt   DATETIME2(0)      NOT NULL,
    ConsumedAt  DATETIME2(0)      NULL,       -- NULL until the token is used to set a new password
    CreatedAt   DATETIME2(0)      NOT NULL CONSTRAINT DF_PasswordResetToken_CreatedAt DEFAULT (SYSDATETIME()),
    RequestIP   NVARCHAR(45)      NULL,       -- 45 chars fits an IPv6 address; same convention as FailedLoginAttempt.IPAddress

    CONSTRAINT PK_PasswordResetToken PRIMARY KEY (TokenID),
    CONSTRAINT UQ_PasswordResetToken_TokenHash UNIQUE (TokenHash),
    CONSTRAINT FK_PasswordResetToken_AppUser FOREIGN KEY (UserID)
        REFERENCES dbo.AppUser (UserID) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT CK_PasswordResetToken_TokenHashFormat CHECK (LEN(TokenHash) = 64 AND TokenHash NOT LIKE N'%[^0-9a-f]%'),
    CONSTRAINT CK_PasswordResetToken_ExpiresAfterCreated CHECK (ExpiresAt > CreatedAt),
    CONSTRAINT CK_PasswordResetToken_ConsumedAfterCreated CHECK (ConsumedAt IS NULL OR ConsumedAt >= CreatedAt)
);
GO

-- The request handler's lookup: "the live, unused token matching this hash".
CREATE NONCLUSTERED INDEX IX_PasswordResetToken_Lookup
    ON dbo.PasswordResetToken (TokenHash) WHERE ConsumedAt IS NULL;
GO

/* =============================================================================
   2. CATALOG AND INVENTORY
   ============================================================================= */

CREATE TABLE dbo.Publisher (
    PublisherID   INT IDENTITY(1,1) NOT NULL,
    PublisherName NVARCHAR(150)     NOT NULL,
    Website       NVARCHAR(255)     NULL,
    IsActive      BIT               NOT NULL CONSTRAINT DF_Publisher_IsActive DEFAULT (1),

    CONSTRAINT PK_Publisher               PRIMARY KEY (PublisherID),
    CONSTRAINT UQ_Publisher_PublisherName UNIQUE (PublisherName),
    CONSTRAINT CK_Publisher_WebsiteFormat CHECK (Website IS NULL OR Website LIKE N'http%://_%')
);
GO

CREATE TABLE dbo.Author (
    AuthorID   INT IDENTITY(1,1) NOT NULL,
    AuthorName NVARCHAR(150)     NOT NULL,
    Biography  NVARCHAR(MAX)     NULL,
    IsActive   BIT               NOT NULL CONSTRAINT DF_Author_IsActive DEFAULT (1),

    CONSTRAINT PK_Author PRIMARY KEY (AuthorID),
    CONSTRAINT CK_Author_NameNotBlank CHECK (LEN(TRIM(AuthorName)) > 0)
);
GO

CREATE TABLE dbo.Category (
    CategoryID   INT IDENTITY(1,1) NOT NULL,
    CategoryName NVARCHAR(100)     NOT NULL,
    Description  NVARCHAR(500)     NULL,
    IsActive     BIT               NOT NULL CONSTRAINT DF_Category_IsActive DEFAULT (1),

    CONSTRAINT PK_Category              PRIMARY KEY (CategoryID),
    CONSTRAINT UQ_Category_CategoryName UNIQUE (CategoryName)
);
GO

-- No AverageRating or AvailableCopies column: both are aggregates (R23).
CREATE TABLE dbo.Book (
    BookID          INT IDENTITY(1,1) NOT NULL,
    ISBN13          NVARCHAR(13)      NOT NULL,
    ISBN10          NVARCHAR(10)      NULL,
    Title           NVARCHAR(255)     NOT NULL,
    Subtitle        NVARCHAR(255)     NULL,
    PublisherID     INT               NULL,       -- 1:N PUBLISHED BY (R9)
    PublicationYear SMALLINT          NULL,
    Edition         NVARCHAR(50)      NULL,
    LanguageCode    NVARCHAR(3)       NOT NULL CONSTRAINT DF_Book_LanguageCode DEFAULT (N'en'),
    Description     NVARCHAR(MAX)     NULL,
    IsActive        BIT               NOT NULL CONSTRAINT DF_Book_IsActive DEFAULT (1),

    CONSTRAINT PK_Book        PRIMARY KEY (BookID),
    CONSTRAINT UQ_Book_ISBN13 UNIQUE (ISBN13),     -- UC-02: duplicate ISBN is an error
    CONSTRAINT FK_Book_Publisher FOREIGN KEY (PublisherID)
        REFERENCES dbo.Publisher (PublisherID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_Book_ISBN13Format CHECK (LEN(ISBN13) = 13 AND ISBN13 NOT LIKE N'%[^0-9]%'),
    CONSTRAINT CK_Book_ISBN10Format CHECK (
        ISBN10 IS NULL
        OR (LEN(ISBN10) = 10 AND LEFT(ISBN10, 9) NOT LIKE N'%[^0-9]%' AND RIGHT(ISBN10, 1) LIKE N'[0-9X]')
    ),
    CONSTRAINT CK_Book_TitleNotBlank      CHECK (LEN(TRIM(Title)) > 0),
    CONSTRAINT CK_Book_PublicationYear    CHECK (PublicationYear IS NULL OR PublicationYear BETWEEN 1450 AND YEAR(SYSDATETIME()) + 1),
    CONSTRAINT CK_Book_LanguageCodeFormat CHECK (LEN(LanguageCode) BETWEEN 2 AND 3 AND LanguageCode NOT LIKE N'%[^a-z]%')
);
GO

CREATE UNIQUE NONCLUSTERED INDEX UX_Book_ISBN10
    ON dbo.Book (ISBN10) WHERE ISBN10 IS NOT NULL;
GO

-- M:N WRITES; AuthorOrder is the relationship's own attribute (R5).
CREATE TABLE dbo.BookAuthor (
    BookID      INT     NOT NULL,
    AuthorID    INT     NOT NULL,
    AuthorOrder TINYINT NOT NULL,

    CONSTRAINT PK_BookAuthor PRIMARY KEY (BookID, AuthorID),
    CONSTRAINT UQ_BookAuthor_BookID_AuthorOrder UNIQUE (BookID, AuthorOrder),
    CONSTRAINT FK_BookAuthor_Book FOREIGN KEY (BookID)
        REFERENCES dbo.Book (BookID) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT FK_BookAuthor_Author FOREIGN KEY (AuthorID)
        REFERENCES dbo.Author (AuthorID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_BookAuthor_AuthorOrder CHECK (AuthorOrder >= 1)
);
GO

-- M:N CLASSIFIES (R6).
CREATE TABLE dbo.BookCategory (
    BookID     INT NOT NULL,
    CategoryID INT NOT NULL,

    CONSTRAINT PK_BookCategory PRIMARY KEY (BookID, CategoryID),
    CONSTRAINT FK_BookCategory_Book FOREIGN KEY (BookID)
        REFERENCES dbo.Book (BookID) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT FK_BookCategory_Category FOREIGN KEY (CategoryID)
        REFERENCES dbo.Category (CategoryID) ON DELETE NO ACTION ON UPDATE NO ACTION
);
GO

-- Multivalued attribute [M] Keywords (R1). The composite PK blocks duplicate keywords.
CREATE TABLE dbo.BookKeyword (
    BookID  INT          NOT NULL,
    Keyword NVARCHAR(50) NOT NULL,

    CONSTRAINT PK_BookKeyword PRIMARY KEY (BookID, Keyword),
    CONSTRAINT FK_BookKeyword_Book FOREIGN KEY (BookID)
        REFERENCES dbo.Book (BookID) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT CK_BookKeyword_NotBlank CHECK (LEN(TRIM(Keyword)) > 0)
);
GO

-- Status is a kept status column (R24). It mixes decision states (Lost,
-- Withdrawn, Under Repair) with loan-driven ones. [D5] triggers on Loan,
-- Reservation and BookIncident are its only writers.
CREATE TABLE dbo.BookCopy (
    CopyID          INT IDENTITY(1,1) NOT NULL,
    BookID          INT               NOT NULL,
    AccessionNumber NVARCHAR(30)      NOT NULL,
    Barcode         NVARCHAR(50)      NOT NULL,
    ShelfLocation   NVARCHAR(50)      NULL,
    AcquisitionDate DATE              NOT NULL,
    -- Per-copy replacement cost for lost/damaged charges (business-rules §5).
    PurchasePrice   DECIMAL(10,2)     NOT NULL,
    CopyCondition   NVARCHAR(10)      NOT NULL CONSTRAINT DF_BookCopy_CopyCondition   DEFAULT (N'Good'),
    Status          NVARCHAR(20)      NOT NULL CONSTRAINT DF_BookCopy_Status          DEFAULT (N'Available'),
    IsReferenceOnly BIT               NOT NULL CONSTRAINT DF_BookCopy_IsReferenceOnly DEFAULT (0),

    CONSTRAINT PK_BookCopy                 PRIMARY KEY (CopyID),
    CONSTRAINT UQ_BookCopy_AccessionNumber UNIQUE (AccessionNumber),
    CONSTRAINT UQ_BookCopy_Barcode         UNIQUE (Barcode),
    CONSTRAINT FK_BookCopy_Book FOREIGN KEY (BookID)
        REFERENCES dbo.Book (BookID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_BookCopy_PurchasePrice CHECK (PurchasePrice >= 0),
    CONSTRAINT CK_BookCopy_CopyCondition CHECK (CopyCondition IN (N'New', N'Good', N'Fair', N'Poor')),
    -- 'Under Repair', 'Damaged' and 'Removed' (here 'Withdrawn') come from the Library Director's interview.
    CONSTRAINT CK_BookCopy_Status CHECK (Status IN (N'Available', N'On Loan', N'On Hold', N'Under Repair', N'Damaged', N'Lost', N'Withdrawn')),
    CONSTRAINT CK_BookCopy_AcquisitionNotFuture CHECK (AcquisitionDate <= CAST(SYSDATETIME() AS DATE)),
    CONSTRAINT CK_BookCopy_ReferenceNotCirculating CHECK (IsReferenceOnly = 0 OR Status NOT IN (N'On Loan', N'On Hold'))
);
GO

/* =============================================================================
   3. CIRCULATION
   ============================================================================= */

/* Aggregation MEMBER—BORROWS—BOOK_COPY (mapping doc §2).
   - LoanID is the surrogate key that RENEWALS, OVERDUE SOURCE and ARISES FROM
     reference.
   - (CopyID, BorrowedAt) is the natural candidate key.
   - No RenewalCount column (R14). Status never holds 'Overdue': that is
     ReturnedAt IS NULL AND DueAt < now (R15).
   [D5] issue procedure checks the borrowing limit, overdue loans, unpaid
   overdue fines, membership expiry and reference-only copies, and sets
   DueAt from 'Loan.PeriodDays'. */
CREATE TABLE dbo.Loan (
    LoanID            INT IDENTITY(1,1) NOT NULL,
    MemberID          INT               NOT NULL,
    CopyID            INT               NOT NULL,
    IssuedByStaffID   INT               NOT NULL,
    ReturnedToStaffID INT               NULL,
    BorrowedAt        DATETIME2(0)      NOT NULL CONSTRAINT DF_Loan_BorrowedAt DEFAULT (SYSDATETIME()),
    DueAt             DATETIME2(0)      NOT NULL,
    ReturnedAt        DATETIME2(0)      NULL,
    ReturnCondition   NVARCHAR(10)      NULL,
    Status            NVARCHAR(10)      NOT NULL CONSTRAINT DF_Loan_Status DEFAULT (N'Active'),

    CONSTRAINT PK_Loan                   PRIMARY KEY (LoanID),
    CONSTRAINT UQ_Loan_CopyID_BorrowedAt UNIQUE (CopyID, BorrowedAt),
    CONSTRAINT FK_Loan_Member FOREIGN KEY (MemberID)
        REFERENCES dbo.Member (MemberID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_Loan_BookCopy FOREIGN KEY (CopyID)
        REFERENCES dbo.BookCopy (CopyID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    -- Staff FKs target StaffProfile, not AppUser, so a member-only account
    -- can never be recorded as issuer (R27).
    CONSTRAINT FK_Loan_IssuedByStaff FOREIGN KEY (IssuedByStaffID)
        REFERENCES dbo.StaffProfile (StaffID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_Loan_ReturnedToStaff FOREIGN KEY (ReturnedToStaffID)
        REFERENCES dbo.StaffProfile (StaffID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_Loan_Status          CHECK (Status IN (N'Active', N'Returned', N'Lost')),
    CONSTRAINT CK_Loan_ReturnCondition CHECK (ReturnCondition IS NULL OR ReturnCondition IN (N'Good', N'Fair', N'Poor', N'Damaged')),
    CONSTRAINT CK_Loan_DueAfterBorrowed      CHECK (DueAt > BorrowedAt),
    CONSTRAINT CK_Loan_ReturnedAfterBorrowed CHECK (ReturnedAt IS NULL OR ReturnedAt >= BorrowedAt),
    -- The three return fields are recorded together or not at all.
    CONSTRAINT CK_Loan_ReturnFieldsTogether CHECK (
        (ReturnedAt IS NULL     AND ReturnedToStaffID IS NULL     AND ReturnCondition IS NULL)
        OR (ReturnedAt IS NOT NULL AND ReturnedToStaffID IS NOT NULL AND ReturnCondition IS NOT NULL)
    ),
    -- 'Returned' if and only if a return was recorded. Active and Lost loans stay open.
    CONSTRAINT CK_Loan_StatusMatchesReturn CHECK (
        (Status = N'Returned' AND ReturnedAt IS NOT NULL)
        OR (Status IN (N'Active', N'Lost') AND ReturnedAt IS NULL)
    )
);
GO

-- A copy has at most one open loan. A lost-but-unreturned loan also counts as open.
CREATE UNIQUE NONCLUSTERED INDEX UX_Loan_OneOpenLoanPerCopy
    ON dbo.Loan (CopyID) WHERE ReturnedAt IS NULL;
GO

-- [D5] at most 2 approved renewals per loan ('Renewal.MaxPerLoan').
-- [D5] refuse a renewal if another member has a Waiting/Ready reservation on the title.
-- [D5] on approval: NewDueAt = OldDueAt + 'Renewal.ExtensionDays', and Loan.DueAt = NewDueAt.
CREATE TABLE dbo.LoanRenewal (
    RenewalID         INT IDENTITY(1,1) NOT NULL,
    LoanID            INT               NOT NULL,
    RequestedByUserID INT               NOT NULL,   -- the member, or a librarian on their behalf
    RequestedAt       DATETIME2(0)      NOT NULL CONSTRAINT DF_LoanRenewal_RequestedAt DEFAULT (SYSDATETIME()),
    ApprovedByStaffID INT               NULL,       -- NULL when auto-approved by the system
    OldDueAt          DATETIME2(0)      NOT NULL,
    NewDueAt          DATETIME2(0)      NOT NULL,
    Status            NVARCHAR(10)      NOT NULL CONSTRAINT DF_LoanRenewal_Status DEFAULT (N'Pending'),

    CONSTRAINT PK_LoanRenewal PRIMARY KEY (RenewalID),
    CONSTRAINT FK_LoanRenewal_Loan FOREIGN KEY (LoanID)
        REFERENCES dbo.Loan (LoanID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_LoanRenewal_RequestedBy FOREIGN KEY (RequestedByUserID)
        REFERENCES dbo.AppUser (UserID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_LoanRenewal_ApprovedByStaff FOREIGN KEY (ApprovedByStaffID)
        REFERENCES dbo.StaffProfile (StaffID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_LoanRenewal_Status CHECK (Status IN (N'Pending', N'Approved', N'Rejected')),
    CONSTRAINT CK_LoanRenewal_NewDueAfterOld CHECK (NewDueAt > OldDueAt),
    CONSTRAINT CK_LoanRenewal_ApproverOnlyWhenApproved CHECK (ApprovedByStaffID IS NULL OR Status = N'Approved')
);
GO

/* Queue per title, FIFO (business-rules §3).
   - No QueuePosition column (R12): the position is
     ROW_NUMBER() OVER (PARTITION BY BookID ORDER BY RequestedAt).
   - No Priority column (R13).
   - [D5] ExpiresAt = ReadyAt + 'Reservation.HoldDays'.
   - [D5] refuse the reservation if an available copy exists. */
CREATE TABLE dbo.Reservation (
    ReservationID INT IDENTITY(1,1) NOT NULL,
    BookID        INT               NOT NULL,
    MemberID      INT               NOT NULL,
    RequestedAt   DATETIME2(0)      NOT NULL CONSTRAINT DF_Reservation_RequestedAt DEFAULT (SYSDATETIME()),
    Status        NVARCHAR(10)      NOT NULL CONSTRAINT DF_Reservation_Status DEFAULT (N'Waiting'),
    ReadyAt       DATETIME2(0)      NULL,
    ExpiresAt     DATETIME2(0)      NULL,
    ClosedAt      DATETIME2(0)      NULL,

    CONSTRAINT PK_Reservation PRIMARY KEY (ReservationID),
    CONSTRAINT FK_Reservation_Book FOREIGN KEY (BookID)
        REFERENCES dbo.Book (BookID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_Reservation_Member FOREIGN KEY (MemberID)
        REFERENCES dbo.Member (MemberID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_Reservation_Status CHECK (Status IN (N'Waiting', N'Ready', N'Fulfilled', N'Cancelled', N'Expired')),
    -- A hold window is recorded as a pair and must run forwards.
    CONSTRAINT CK_Reservation_HoldWindow CHECK (
        (ReadyAt IS NULL AND ExpiresAt IS NULL)
        OR (ReadyAt IS NOT NULL AND ExpiresAt IS NOT NULL AND ExpiresAt > ReadyAt)
    ),
    -- Waiting has no hold yet. Ready, Fulfilled and Expired must have had one.
    -- Cancelled can happen either before or after a copy was held.
    CONSTRAINT CK_Reservation_StatusMatchesHold CHECK (
        (Status = N'Waiting' AND ReadyAt IS NULL)
        OR (Status IN (N'Ready', N'Fulfilled', N'Expired') AND ReadyAt IS NOT NULL)
        OR (Status = N'Cancelled')
    ),
    CONSTRAINT CK_Reservation_ClosedMatchesStatus CHECK (
        (Status IN (N'Waiting', N'Ready') AND ClosedAt IS NULL)
        OR (Status IN (N'Fulfilled', N'Cancelled', N'Expired') AND ClosedAt IS NOT NULL)
    ),
    CONSTRAINT CK_Reservation_ReadyAfterRequested  CHECK (ReadyAt  IS NULL OR ReadyAt  >= RequestedAt),
    CONSTRAINT CK_Reservation_ClosedAfterRequested CHECK (ClosedAt IS NULL OR ClosedAt >= RequestedAt)
);
GO

-- UC-04: a member cannot hold two active reservations for the same title.
CREATE UNIQUE NONCLUSTERED INDEX UX_Reservation_OneActivePerMemberBook
    ON dbo.Reservation (BookID, MemberID) WHERE Status IN (N'Waiting', N'Ready');
GO

-- Supports the FIFO queue read (next Waiting reservation for a title).
CREATE NONCLUSTERED INDEX IX_Reservation_Queue
    ON dbo.Reservation (BookID, RequestedAt) WHERE Status = N'Waiting';
GO

/* =============================================================================
   4. FINES, APPEALS AND PAYMENTS
   ============================================================================= */

-- No ReplacementCost or RepairCharge columns: the charge lives only in Fine.AmountAssessed (R21).
-- CopyID and MemberID are kept even when LoanID is present, because LoanID is
-- optional: an incident found on the shelf has no loan (R20).
-- [D5] if LoanID IS NOT NULL, CopyID and MemberID must equal the loan's.
CREATE TABLE dbo.BookIncident (
    IncidentID        INT IDENTITY(1,1) NOT NULL,
    CopyID            INT               NOT NULL,   -- AFFECTS
    LoanID            INT               NULL,       -- ARISES FROM
    MemberID          INT               NULL,       -- LIABLE MEMBER
    RecordedByStaffID INT               NOT NULL,
    IncidentType      NVARCHAR(10)      NOT NULL,
    Description       NVARCHAR(1000)    NOT NULL,   -- librarian's justification note (business-rules §5)
    ReportedAt        DATETIME2(0)      NOT NULL CONSTRAINT DF_BookIncident_ReportedAt DEFAULT (SYSDATETIME()),
    Status            NVARCHAR(15)      NOT NULL CONSTRAINT DF_BookIncident_Status DEFAULT (N'Open'),

    CONSTRAINT PK_BookIncident PRIMARY KEY (IncidentID),
    CONSTRAINT FK_BookIncident_BookCopy FOREIGN KEY (CopyID)
        REFERENCES dbo.BookCopy (CopyID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_BookIncident_Loan FOREIGN KEY (LoanID)
        REFERENCES dbo.Loan (LoanID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_BookIncident_Member FOREIGN KEY (MemberID)
        REFERENCES dbo.Member (MemberID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_BookIncident_RecordedByStaff FOREIGN KEY (RecordedByStaffID)
        REFERENCES dbo.StaffProfile (StaffID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_BookIncident_IncidentType CHECK (IncidentType IN (N'Lost', N'Damaged')),
    CONSTRAINT CK_BookIncident_Status       CHECK (Status IN (N'Open', N'Charged', N'Resolved', N'Written Off')),
    CONSTRAINT CK_BookIncident_DescriptionNotBlank CHECK (LEN(TRIM(Description)) > 0)
);
GO

/* One row per charge. The source is EITHER a loan (Overdue) OR an incident
   (Lost/Damaged), never both.
   - No DaysOverdue column (R16): derive it from Loan.DueAt / ReturnedAt.
   - No WaivedAmount column (R17): partial reductions come from approved
     FineAppeal rows.
   - Outstanding balance is never stored (R18):
       AmountAssessed - SUM(approved ApprovedReduction) - SUM(Completed AmountPaid)
   - RatePerDay and AmountAssessed are snapshots of policy at assessment
     time, so a later rate or cap change does not rewrite old fines.
   - Status is a kept status column (R24). [D5] triggers on FinePayment and
     FineAppeal are its only writers.
   - [D5] Overdue: AmountAssessed = MIN(days x RatePerDay, 'Fine.MaxPerLoan').
     Lost: PurchasePrice + 'Fine.LostBookProcessingFee'.
     Damaged: at most PurchasePrice.
   - [D5] MemberID must equal the loan's member (Overdue) or the incident's
     liable member (Lost/Damaged). */
CREATE TABLE dbo.Fine (
    FineID          INT IDENTITY(1,1) NOT NULL,
    MemberID        INT               NOT NULL,   -- CHARGED TO
    LoanID          INT               NULL,       -- OVERDUE SOURCE
    IncidentID      INT               NULL,       -- LOSS/DAMAGE SOURCE
    FineType        NVARCHAR(10)      NOT NULL,
    RatePerDay      DECIMAL(10,2)     NULL,
    AmountAssessed  DECIMAL(10,2)     NOT NULL,
    AssessedAt      DATETIME2(0)      NOT NULL CONSTRAINT DF_Fine_AssessedAt DEFAULT (SYSDATETIME()),
    Status          NVARCHAR(15)      NOT NULL CONSTRAINT DF_Fine_Status DEFAULT (N'Pending'),
    WaiverReason    NVARCHAR(500)     NULL,
    WaivedByStaffID INT               NULL,

    CONSTRAINT PK_Fine PRIMARY KEY (FineID),
    CONSTRAINT FK_Fine_Member FOREIGN KEY (MemberID)
        REFERENCES dbo.Member (MemberID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_Fine_Loan FOREIGN KEY (LoanID)
        REFERENCES dbo.Loan (LoanID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_Fine_BookIncident FOREIGN KEY (IncidentID)
        REFERENCES dbo.BookIncident (IncidentID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_Fine_WaivedByStaff FOREIGN KEY (WaivedByStaffID)
        REFERENCES dbo.StaffProfile (StaffID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_Fine_FineType CHECK (FineType IN (N'Overdue', N'Lost', N'Damaged')),
    CONSTRAINT CK_Fine_Status   CHECK (Status IN (N'Pending', N'Under Appeal', N'Partially Paid', N'Fully Paid', N'Waived')),
    -- Exactly one source, matched to the fine type. For incident fines, the
    -- loan is reached through the incident, so it is not stored twice here.
    CONSTRAINT CK_Fine_SourceMatchesType CHECK (
        (FineType = N'Overdue' AND LoanID IS NOT NULL AND IncidentID IS NULL AND RatePerDay IS NOT NULL)
        OR (FineType IN (N'Lost', N'Damaged') AND IncidentID IS NOT NULL AND LoanID IS NULL AND RatePerDay IS NULL)
    ),
    CONSTRAINT CK_Fine_AmountAssessed CHECK (AmountAssessed > 0),
    CONSTRAINT CK_Fine_RatePerDay     CHECK (RatePerDay IS NULL OR RatePerDay > 0),
    -- A full waiver must say who waived it and why (business-rules §2).
    CONSTRAINT CK_Fine_WaiverDetails CHECK (
        (Status = N'Waived' AND WaiverReason IS NOT NULL AND WaivedByStaffID IS NOT NULL)
        OR (Status <> N'Waived' AND WaiverReason IS NULL AND WaivedByStaffID IS NULL)
    )
);
GO

-- One overdue fine per loan, so the per-loan cap applies to a single row (R29).
CREATE UNIQUE NONCLUSTERED INDEX UX_Fine_OneOverduePerLoan
    ON dbo.Fine (LoanID) WHERE FineType = N'Overdue';
GO

CREATE UNIQUE NONCLUSTERED INDEX UX_Fine_OnePerIncident
    ON dbo.Fine (IncidentID) WHERE IncidentID IS NOT NULL;
GO

-- No MemberID column (R19): FineID is NOT NULL, and the appellant is always the fined member.
-- [D5] ApprovedReduction must not exceed the outstanding balance.
-- [D5] a Pending appeal sets Fine.Status = 'Under Appeal'.
CREATE TABLE dbo.FineAppeal (
    AppealID          INT IDENTITY(1,1) NOT NULL,
    FineID            INT               NOT NULL,
    AppealReason      NVARCHAR(1000)    NOT NULL,
    SubmittedAt       DATETIME2(0)      NOT NULL CONSTRAINT DF_FineAppeal_SubmittedAt DEFAULT (SYSDATETIME()),
    Status            NVARCHAR(10)      NOT NULL CONSTRAINT DF_FineAppeal_Status DEFAULT (N'Pending'),
    DecidedByStaffID  INT               NULL,
    DecidedAt         DATETIME2(0)      NULL,
    DecisionComments  NVARCHAR(1000)    NULL,
    ApprovedReduction DECIMAL(10,2)     NULL,

    CONSTRAINT PK_FineAppeal PRIMARY KEY (AppealID),
    CONSTRAINT FK_FineAppeal_Fine FOREIGN KEY (FineID)
        REFERENCES dbo.Fine (FineID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_FineAppeal_DecidedByStaff FOREIGN KEY (DecidedByStaffID)
        REFERENCES dbo.StaffProfile (StaffID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_FineAppeal_Status CHECK (Status IN (N'Pending', N'Approved', N'Rejected')),
    CONSTRAINT CK_FineAppeal_ReasonNotBlank CHECK (LEN(TRIM(AppealReason)) > 0),
    -- Pending: no decision yet. Rejected: decided, no reduction. Approved: decided, with a reduction.
    CONSTRAINT CK_FineAppeal_DecisionMatchesStatus CHECK (
        (Status = N'Pending'  AND DecidedByStaffID IS NULL     AND DecidedAt IS NULL     AND ApprovedReduction IS NULL)
        OR (Status = N'Rejected' AND DecidedByStaffID IS NOT NULL AND DecidedAt IS NOT NULL AND ApprovedReduction IS NULL)
        OR (Status = N'Approved' AND DecidedByStaffID IS NOT NULL AND DecidedAt IS NOT NULL AND ApprovedReduction IS NOT NULL)
    ),
    CONSTRAINT CK_FineAppeal_ApprovedReduction CHECK (ApprovedReduction IS NULL OR ApprovedReduction > 0),
    CONSTRAINT CK_FineAppeal_DecidedAfterSubmitted CHECK (DecidedAt IS NULL OR DecidedAt >= SubmittedAt)
);
GO

-- An appeal pauses collection until decided, so only one may be open per fine.
CREATE UNIQUE NONCLUSTERED INDEX UX_FineAppeal_OnePendingPerFine
    ON dbo.FineAppeal (FineID) WHERE Status = N'Pending';
GO

-- [D5] reject payments while the fine is 'Under Appeal' or 'Waived'.
-- [D5] Completed payments must not exceed the outstanding balance.
-- [D5] each payment updates Fine.Status to 'Partially Paid' or 'Fully Paid'.
CREATE TABLE dbo.FinePayment (
    PaymentID         INT IDENTITY(1,1) NOT NULL,
    FineID            INT               NOT NULL,
    ReceiptNumber     NVARCHAR(30)      NOT NULL,
    AmountPaid        DECIMAL(10,2)     NOT NULL,
    PaymentMethod     NVARCHAR(10)      NOT NULL,
    PaymentStatus     NVARCHAR(10)      NOT NULL CONSTRAINT DF_FinePayment_PaymentStatus DEFAULT (N'Completed'),
    PaidAt            DATETIME2(0)      NOT NULL CONSTRAINT DF_FinePayment_PaidAt DEFAULT (SYSDATETIME()),
    ReceivedByStaffID INT               NULL,       -- NULL for online self-payment

    CONSTRAINT PK_FinePayment               PRIMARY KEY (PaymentID),
    CONSTRAINT UQ_FinePayment_ReceiptNumber UNIQUE (ReceiptNumber),   -- UC-07 unique receipt
    CONSTRAINT FK_FinePayment_Fine FOREIGN KEY (FineID)
        REFERENCES dbo.Fine (FineID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_FinePayment_ReceivedByStaff FOREIGN KEY (ReceivedByStaffID)
        REFERENCES dbo.StaffProfile (StaffID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_FinePayment_AmountPaid    CHECK (AmountPaid > 0),
    CONSTRAINT CK_FinePayment_PaymentMethod CHECK (PaymentMethod IN (N'Cash', N'Card', N'Online')),
    CONSTRAINT CK_FinePayment_PaymentStatus CHECK (PaymentStatus IN (N'Completed', N'Failed', N'Refunded')),
    -- UC-07 alternative flow: cash at the counter is recorded by a staff member.
    CONSTRAINT CK_FinePayment_CashHasReceiver CHECK (PaymentMethod <> N'Cash' OR ReceivedByStaffID IS NOT NULL)
);
GO

/* =============================================================================
   5. REVIEWS AND FEEDBACK
   ============================================================================= */

-- No ModeratedBy column: moderators are recorded per transition in ReviewModerationHistory (R22).
-- [D5] insert only if the member has a Loan of some copy of this book (UC-09 precondition).
CREATE TABLE dbo.BookReview (
    ReviewID    INT IDENTITY(1,1) NOT NULL,
    BookID      INT               NOT NULL,
    MemberID    INT               NOT NULL,
    Rating      TINYINT           NOT NULL,
    ReviewText  NVARCHAR(2000)    NULL,       -- the written review is optional
    Status      NVARCHAR(10)      NOT NULL CONSTRAINT DF_BookReview_Status      DEFAULT (N'Pending'),
    SubmittedAt DATETIME2(0)      NOT NULL CONSTRAINT DF_BookReview_SubmittedAt DEFAULT (SYSDATETIME()),
    UpdatedAt   DATETIME2(0)      NOT NULL CONSTRAINT DF_BookReview_UpdatedAt   DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_BookReview                 PRIMARY KEY (ReviewID),
    CONSTRAINT UQ_BookReview_BookID_MemberID UNIQUE (BookID, MemberID),  -- UC-09: edit, never duplicate
    CONSTRAINT FK_BookReview_Book FOREIGN KEY (BookID)
        REFERENCES dbo.Book (BookID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_BookReview_Member FOREIGN KEY (MemberID)
        REFERENCES dbo.Member (MemberID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_BookReview_Rating CHECK (Rating BETWEEN 1 AND 5),
    CONSTRAINT CK_BookReview_Status CHECK (Status IN (N'Pending', N'Approved', N'Rejected', N'Hidden', N'Removed')),
    CONSTRAINT CK_BookReview_UpdatedAfterSubmitted CHECK (UpdatedAt >= SubmittedAt)
);
GO

-- [D5] reporter must not be the review's author.
-- [D5] a new flag moves an Approved review to 'Hidden'.
CREATE TABLE dbo.ReviewFlag (
    ReviewFlagID       INT IDENTITY(1,1) NOT NULL,
    ReviewID           INT               NOT NULL,
    ReportedByMemberID INT               NOT NULL,
    Reason             NVARCHAR(500)     NOT NULL,
    Status             NVARCHAR(10)      NOT NULL CONSTRAINT DF_ReviewFlag_Status     DEFAULT (N'Open'),
    ReportedAt         DATETIME2(0)      NOT NULL CONSTRAINT DF_ReviewFlag_ReportedAt DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_ReviewFlag                    PRIMARY KEY (ReviewFlagID),
    CONSTRAINT UQ_ReviewFlag_ReviewID_ReportedBy UNIQUE (ReviewID, ReportedByMemberID),
    CONSTRAINT FK_ReviewFlag_BookReview FOREIGN KEY (ReviewID)
        REFERENCES dbo.BookReview (ReviewID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_ReviewFlag_ReportedByMember FOREIGN KEY (ReportedByMemberID)
        REFERENCES dbo.Member (MemberID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_ReviewFlag_Status CHECK (Status IN (N'Open', N'Upheld', N'Dismissed')),
    CONSTRAINT CK_ReviewFlag_ReasonNotBlank CHECK (LEN(TRIM(Reason)) > 0)
);
GO

CREATE TABLE dbo.ReviewModerationHistory (
    ModerationID     INT IDENTITY(1,1) NOT NULL,
    ReviewID         INT               NOT NULL,
    PreviousStatus   NVARCHAR(10)      NOT NULL,
    NewStatus        NVARCHAR(10)      NOT NULL,
    Reason           NVARCHAR(500)     NULL,
    -- NULL only for the automatic Approved -> Hidden transition fired by a new flag.
    ModeratorStaffID INT               NULL,
    ModeratedAt      DATETIME2(0)      NOT NULL CONSTRAINT DF_ReviewModerationHistory_ModeratedAt DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_ReviewModerationHistory PRIMARY KEY (ModerationID),
    CONSTRAINT FK_ReviewModerationHistory_BookReview FOREIGN KEY (ReviewID)
        REFERENCES dbo.BookReview (ReviewID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_ReviewModerationHistory_ModeratorStaff FOREIGN KEY (ModeratorStaffID)
        REFERENCES dbo.StaffProfile (StaffID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_ReviewModerationHistory_PreviousStatus CHECK (PreviousStatus IN (N'Pending', N'Approved', N'Rejected', N'Hidden', N'Removed')),
    CONSTRAINT CK_ReviewModerationHistory_NewStatus      CHECK (NewStatus      IN (N'Pending', N'Approved', N'Rejected', N'Hidden', N'Removed')),
    CONSTRAINT CK_ReviewModerationHistory_StatusChanged  CHECK (PreviousStatus <> NewStatus),
    CONSTRAINT CK_ReviewModerationHistory_RejectHasReason CHECK (NewStatus <> N'Rejected' OR Reason IS NOT NULL),
    CONSTRAINT CK_ReviewModerationHistory_ModeratorRequired CHECK (ModeratorStaffID IS NOT NULL OR NewStatus = N'Hidden')
);
GO

CREATE TABLE dbo.FeedbackCategory (
    FeedbackCategoryID INT IDENTITY(1,1) NOT NULL,
    CategoryName       NVARCHAR(50)      NOT NULL,
    IsActive           BIT               NOT NULL CONSTRAINT DF_FeedbackCategory_IsActive DEFAULT (1),

    CONSTRAINT PK_FeedbackCategory              PRIMARY KEY (FeedbackCategoryID),
    CONSTRAINT UQ_FeedbackCategory_CategoryName UNIQUE (CategoryName)
);
GO

-- [D5] the member may edit only while Status = 'Submitted' AND AdminResponse IS NULL (UC-10).
CREATE TABLE dbo.MemberFeedback (
    FeedbackID         INT IDENTITY(1,1) NOT NULL,
    FeedbackReference  NVARCHAR(20)      NOT NULL,
    MemberID           INT               NOT NULL,
    FeedbackCategoryID INT               NOT NULL,
    Subject            NVARCHAR(150)     NOT NULL,
    Description        NVARCHAR(2000)    NOT NULL,
    Priority           NVARCHAR(10)      NOT NULL CONSTRAINT DF_MemberFeedback_Priority    DEFAULT (N'Medium'),
    Status             NVARCHAR(15)      NOT NULL CONSTRAINT DF_MemberFeedback_Status      DEFAULT (N'Submitted'),
    AdminResponse      NVARCHAR(2000)    NULL,
    SubmittedAt        DATETIME2(0)      NOT NULL CONSTRAINT DF_MemberFeedback_SubmittedAt DEFAULT (SYSDATETIME()),
    -- The member-services interview asks to "see the date of the latest update".
    UpdatedAt          DATETIME2(0)      NOT NULL CONSTRAINT DF_MemberFeedback_UpdatedAt   DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_MemberFeedback                   PRIMARY KEY (FeedbackID),
    CONSTRAINT UQ_MemberFeedback_FeedbackReference UNIQUE (FeedbackReference),
    CONSTRAINT FK_MemberFeedback_Member FOREIGN KEY (MemberID)
        REFERENCES dbo.Member (MemberID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_MemberFeedback_FeedbackCategory FOREIGN KEY (FeedbackCategoryID)
        REFERENCES dbo.FeedbackCategory (FeedbackCategoryID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_MemberFeedback_Priority CHECK (Priority IN (N'Low', N'Medium', N'High')),
    -- business-rules §6 lifecycle.
    CONSTRAINT CK_MemberFeedback_Status   CHECK (Status IN (N'Submitted', N'Under Review', N'In Progress', N'Resolved', N'Closed')),
    CONSTRAINT CK_MemberFeedback_NotBlank CHECK (LEN(TRIM(Subject)) > 0 AND LEN(TRIM(Description)) > 0),
    CONSTRAINT CK_MemberFeedback_UpdatedAfterSubmitted CHECK (UpdatedAt >= SubmittedAt)
);
GO

CREATE TABLE dbo.FeedbackHistory (
    FeedbackHistoryID INT IDENTITY(1,1) NOT NULL,
    FeedbackID        INT               NOT NULL,
    PreviousStatus    NVARCHAR(15)      NOT NULL,
    NewStatus         NVARCHAR(15)      NOT NULL,
    PreviousResponse  NVARCHAR(2000)    NULL,
    NewResponse       NVARCHAR(2000)    NULL,
    ChangedByUserID   INT               NOT NULL,
    ChangedAt         DATETIME2(0)      NOT NULL CONSTRAINT DF_FeedbackHistory_ChangedAt DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_FeedbackHistory PRIMARY KEY (FeedbackHistoryID),
    -- CASCADE: history is owned by its feedback. UC-10 lets a member withdraw
    -- unreviewed feedback, which deletes the feedback row.
    CONSTRAINT FK_FeedbackHistory_MemberFeedback FOREIGN KEY (FeedbackID)
        REFERENCES dbo.MemberFeedback (FeedbackID) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT FK_FeedbackHistory_ChangedBy FOREIGN KEY (ChangedByUserID)
        REFERENCES dbo.AppUser (UserID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_FeedbackHistory_PreviousStatus CHECK (PreviousStatus IN (N'Submitted', N'Under Review', N'In Progress', N'Resolved', N'Closed')),
    CONSTRAINT CK_FeedbackHistory_NewStatus      CHECK (NewStatus      IN (N'Submitted', N'Under Review', N'In Progress', N'Resolved', N'Closed')),
    -- A history row must record an actual change of status or response.
    CONSTRAINT CK_FeedbackHistory_SomethingChanged CHECK (
        PreviousStatus <> NewStatus
        OR ISNULL(PreviousResponse, N'') <> ISNULL(NewResponse, N'')
    )
);
GO

/* =============================================================================
   6. OPERATIONS AND GOVERNANCE
   ============================================================================= */

CREATE TABLE dbo.Notification (
    NotificationID   INT IDENTITY(1,1) NOT NULL,
    UserID           INT               NOT NULL,
    NotificationType NVARCHAR(30)      NOT NULL,
    Title            NVARCHAR(150)     NOT NULL,
    Message          NVARCHAR(1000)    NOT NULL,
    IsRead           BIT               NOT NULL CONSTRAINT DF_Notification_IsRead    DEFAULT (0),
    CreatedAt        DATETIME2(0)      NOT NULL CONSTRAINT DF_Notification_CreatedAt DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_Notification PRIMARY KEY (NotificationID),
    CONSTRAINT FK_Notification_AppUser FOREIGN KEY (UserID)
        REFERENCES dbo.AppUser (UserID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_Notification_NotificationType CHECK (NotificationType IN (
        N'ReservationReady', N'DueSoon', N'Overdue', N'FineIssued', N'AppealDecision',
        N'RenewalDecision', N'ReviewDecision', N'FeedbackResponse', N'General'))
);
GO

CREATE TABLE dbo.AuditLog (
    AuditID    BIGINT IDENTITY(1,1) NOT NULL,
    UserID     INT                  NULL,       -- NULL for system or trigger actions
    ActionName NVARCHAR(50)         NOT NULL,
    EntityName NVARCHAR(50)         NOT NULL,
    EntityID   NVARCHAR(100)        NULL,       -- NVARCHAR because SystemSetting's key is text
    -- SQL Server 2022 has no JSON data type; JSON is stored as NVARCHAR and validated.
    Details    NVARCHAR(MAX)        NULL,
    OccurredAt DATETIME2(0)         NOT NULL CONSTRAINT DF_AuditLog_OccurredAt DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_AuditLog PRIMARY KEY (AuditID),
    CONSTRAINT FK_AuditLog_AppUser FOREIGN KEY (UserID)
        REFERENCES dbo.AppUser (UserID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_AuditLog_DetailsIsJson CHECK (Details IS NULL OR ISJSON(Details) = 1)
);
GO

CREATE TABLE dbo.ReportAudit (
    ReportAuditID     INT IDENTITY(1,1) NOT NULL,
    RequestedByUserID INT               NOT NULL,
    ReportType        NVARCHAR(30)      NOT NULL,
    FilterJson        NVARCHAR(MAX)     NULL,
    GeneratedAt       DATETIME2(0)      NOT NULL CONSTRAINT DF_ReportAudit_GeneratedAt DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_ReportAudit PRIMARY KEY (ReportAuditID),
    CONSTRAINT FK_ReportAudit_RequestedBy FOREIGN KEY (RequestedByUserID)
        REFERENCES dbo.AppUser (UserID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    -- Report list from UC-08 plus the Administrator and Finance interviews.
    CONSTRAINT CK_ReportAudit_ReportType CHECK (ReportType IN (
        N'BookInventory', N'BorrowedBooks', N'ReturnedBooks', N'OverdueBooks', N'Reservations',
        N'MemberRegistration', N'MemberActivity', N'MostBorrowedBooks', N'LostDamagedBooks',
        N'FineCollection', N'OutstandingFines', N'FinePaymentHistory', N'WaivedFines', N'Revenue',
        N'Reviews', N'Feedback')),
    CONSTRAINT CK_ReportAudit_FilterIsJson CHECK (FilterJson IS NULL OR ISJSON(FilterJson) = 1)
);
GO

-- Configurable policy values (business-rules §7). The natural key is the setting name.
CREATE TABLE dbo.SystemSetting (
    SettingKey      NVARCHAR(100) NOT NULL,
    SettingValue    NVARCHAR(255) NOT NULL,
    SettingDataType NVARCHAR(10)  NOT NULL,
    Description     NVARCHAR(500) NULL,
    UpdatedByUserID INT           NULL,       -- NULL for values seeded by this script
    UpdatedAt       DATETIME2(0)  NOT NULL CONSTRAINT DF_SystemSetting_UpdatedAt DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_SystemSetting PRIMARY KEY (SettingKey),
    CONSTRAINT FK_SystemSetting_UpdatedBy FOREIGN KEY (UpdatedByUserID)
        REFERENCES dbo.AppUser (UserID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_SystemSetting_DataType CHECK (SettingDataType IN (N'Int', N'Decimal', N'String', N'Bool')),
    -- The stored text must parse as its declared type, so a procedure reading
    -- 'Loan.PeriodDays' can never get 'fourteen'.
    CONSTRAINT CK_SystemSetting_ValueMatchesType CHECK (
        (SettingDataType = N'Int'     AND TRY_CAST(SettingValue AS INT) IS NOT NULL)
        OR (SettingDataType = N'Decimal' AND TRY_CAST(SettingValue AS DECIMAL(18,4)) IS NOT NULL)
        OR (SettingDataType = N'Bool'    AND SettingValue IN (N'0', N'1'))
        OR (SettingDataType = N'String')
    )
);
GO

-- No FK to AppUser on purpose: EmailTried may belong to no account, and those attempts must still be logged.
CREATE TABLE dbo.FailedLoginAttempt (
    AttemptID     BIGINT IDENTITY(1,1) NOT NULL,
    EmailTried    NVARCHAR(254)        NOT NULL,
    IPAddress     NVARCHAR(45)         NOT NULL,   -- 45 chars fits an IPv6 address
    UserAgent     NVARCHAR(500)        NULL,
    FailureReason NVARCHAR(20)         NOT NULL,
    AttemptedAt   DATETIME2(0)         NOT NULL CONSTRAINT DF_FailedLoginAttempt_AttemptedAt DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_FailedLoginAttempt PRIMARY KEY (AttemptID),
    CONSTRAINT CK_FailedLoginAttempt_FailureReason CHECK (FailureReason IN (N'UnknownEmail', N'BadPassword', N'AccountLocked', N'AccountDeactivated'))
);
GO

CREATE TABLE dbo.DatabaseBackupLog (
    BackupLogID       INT IDENTITY(1,1) NOT NULL,
    BackupType        NVARCHAR(15)      NOT NULL,
    BackupPath        NVARCHAR(400)     NOT NULL,
    StartedAt         DATETIME2(0)      NOT NULL CONSTRAINT DF_DatabaseBackupLog_StartedAt DEFAULT (SYSDATETIME()),
    CompletedAt       DATETIME2(0)      NULL,
    Status            NVARCHAR(10)      NOT NULL CONSTRAINT DF_DatabaseBackupLog_Status DEFAULT (N'Running'),
    InitiatedByUserID INT               NULL,       -- NULL for the scheduled daily backup
    ErrorMessage      NVARCHAR(2000)    NULL,

    CONSTRAINT PK_DatabaseBackupLog PRIMARY KEY (BackupLogID),
    CONSTRAINT FK_DatabaseBackupLog_InitiatedBy FOREIGN KEY (InitiatedByUserID)
        REFERENCES dbo.AppUser (UserID) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_DatabaseBackupLog_BackupType CHECK (BackupType IN (N'Full', N'Differential', N'Log')),
    CONSTRAINT CK_DatabaseBackupLog_Status     CHECK (Status IN (N'Running', N'Succeeded', N'Failed')),
    CONSTRAINT CK_DatabaseBackupLog_CompletedAfterStarted CHECK (CompletedAt IS NULL OR CompletedAt >= StartedAt),
    CONSTRAINT CK_DatabaseBackupLog_CompletedMatchesStatus CHECK (
        (Status = N'Running' AND CompletedAt IS NULL)
        OR (Status IN (N'Succeeded', N'Failed') AND CompletedAt IS NOT NULL)
    ),
    CONSTRAINT CK_DatabaseBackupLog_FailureHasMessage CHECK (Status <> N'Failed' OR ErrorMessage IS NOT NULL)
);
GO

/* =============================================================================
   7. SEED: SystemSetting (values from docs/business-rules.md)
   -----------------------------------------------------------------------------
   These rows are the only place these numbers exist. Procedures and the Java
   services read them, so "what if the loan period became 21 days?" is a
   one-row UPDATE (business-rules §7).

   The borrowing-limit keys end in the exact Member.MemberType values
   ('Student', 'Academic Staff'), so a procedure can look one up with
   CONCAT(N'Borrowing.Limit.', m.MemberType) and needs no CASE.
   ============================================================================= */
INSERT INTO dbo.SystemSetting (SettingKey, SettingValue, SettingDataType, Description)
VALUES
    (N'Loan.PeriodDays',                N'14',     N'Int',     N'Days from issue to due date (business-rules §1).'),
    (N'Fine.RatePerDay',                N'20.00',  N'Decimal', N'Overdue fine in LKR per day, per book (business-rules §2).'),
    (N'Borrowing.Limit.Student',        N'5',      N'Int',     N'Maximum active loans for a Student member (business-rules §1).'),
    (N'Borrowing.Limit.Academic Staff', N'10',     N'Int',     N'Maximum active loans for an Academic Staff member (business-rules §1).'),
    (N'Renewal.MaxPerLoan',             N'2',      N'Int',     N'Maximum approved renewals per loan (business-rules §1).'),
    (N'Renewal.ExtensionDays',          N'14',     N'Int',     N'Days each approved renewal adds to the due date (business-rules §1).'),
    (N'Reservation.HoldDays',           N'3',      N'Int',     N'Days a Ready reservation is held for collection before it expires (business-rules §3).'),
    (N'Fine.MaxPerLoan',                N'500.00', N'Decimal', N'Cap in LKR on the overdue fine for a single loan (business-rules §2).'),
    (N'Fine.LostBookProcessingFee',     N'500.00', N'Decimal', N'Flat LKR fee added to the copy''s PurchasePrice for a lost book (business-rules §5).'),
    (N'Security.LockoutMaxAttempts',    N'5',      N'Int',     N'Failed logins allowed for one email before the account is locked (business-rules §8).'),
    (N'Security.LockoutWindowMinutes',  N'15',     N'Int',     N'Rolling window the failed-login count above is measured over (business-rules §8).'),
    (N'Security.SessionTimeoutMinutes', N'30',     N'Int',     N'Idle minutes before a session expires and the next request must log in again (business-rules §8).'),
    (N'Security.PasswordResetTokenMinutes', N'30', N'Int',     N'Minutes a password-reset link stays valid after it is requested (business-rules §8).');
GO
