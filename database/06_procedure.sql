/* =============================================================================
   sp_IssueBook — UC-03 issue-a-book, enforcing every rule in
   business-rules.md section 1 before a Loan is ever written.

   Error numbers (THROW, all >= 50000, one per distinct rule):
     50001  no such member
     50002  membership not Active
     50003  membership expired
     50004  borrowing limit reached (SystemSetting 'Borrowing.Limit.<MemberType>')
     50005  member has an overdue loan
     50006  member has an unpaid fine from an overdue return
     50007  no such copy
     50008  copy is not Available
     50009  copy is reference-only
     50020  'Borrowing.Limit.<MemberType>' setting missing
     50021  'Loan.PeriodDays' setting missing
   ============================================================================= */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

CREATE OR ALTER PROCEDURE dbo.sp_IssueBook
    @MemberID        INT,
    @CopyID          INT,
    @IssuedByStaffID INT,
    @LoanID          INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        BEGIN TRANSACTION;

        -- UPDLOCK/HOLDLOCK: two simultaneous issues to the same member must
        -- not both read the same active-loan count and both pass the limit
        -- check before either commits.
        DECLARE @MembershipStatus NVARCHAR(20), @ExpiryDate DATE, @MemberType NVARCHAR(20);
        SELECT @MembershipStatus = MembershipStatus, @ExpiryDate = ExpiryDate, @MemberType = MemberType
        FROM dbo.Member WITH (UPDLOCK, HOLDLOCK)
        WHERE MemberID = @MemberID;

        IF @MembershipStatus IS NULL
            THROW 50001, N'sp_IssueBook: no member exists with this MemberID.', 1;

        IF @MembershipStatus <> N'Active'
            THROW 50002, N'sp_IssueBook: this membership is not Active.', 1;

        IF @ExpiryDate < CAST(SYSDATETIME() AS DATE)
            THROW 50003, N'sp_IssueBook: this membership has expired.', 1;

        DECLARE @Limit INT;
        SELECT @Limit = TRY_CAST(SettingValue AS INT)
        FROM dbo.SystemSetting
        WHERE SettingKey = CONCAT(N'Borrowing.Limit.', @MemberType);

        IF @Limit IS NULL
            THROW 50020, N'sp_IssueBook: no borrowing-limit setting found for this member type.', 1;

        DECLARE @ActiveLoanCount INT;
        SELECT @ActiveLoanCount = COUNT(*)
        FROM dbo.Loan
        WHERE MemberID = @MemberID AND Status = N'Active';

        IF @ActiveLoanCount >= @Limit
            THROW 50004, N'sp_IssueBook: this member has reached their borrowing limit.', 1;

        IF EXISTS (
            SELECT 1 FROM dbo.Loan
            WHERE MemberID = @MemberID AND ReturnedAt IS NULL AND DueAt < SYSDATETIME()
        )
            THROW 50005, N'sp_IssueBook: this member has an overdue loan and cannot borrow until it is returned.', 1;

        IF EXISTS (
            SELECT 1 FROM dbo.Fine
            WHERE MemberID = @MemberID AND FineType = N'Overdue' AND Status NOT IN (N'Fully Paid', N'Waived')
        )
            THROW 50006, N'sp_IssueBook: this member has an unpaid fine from an overdue return.', 1;

        DECLARE @CopyStatus NVARCHAR(20), @IsReferenceOnly BIT;
        SELECT @CopyStatus = Status, @IsReferenceOnly = IsReferenceOnly
        FROM dbo.BookCopy WITH (UPDLOCK, HOLDLOCK)
        WHERE CopyID = @CopyID;

        IF @CopyStatus IS NULL
            THROW 50007, N'sp_IssueBook: no book copy exists with this CopyID.', 1;

        IF @CopyStatus <> N'Available'
            THROW 50008, N'sp_IssueBook: this copy is not currently available.', 1;

        IF @IsReferenceOnly = 1
            THROW 50009, N'sp_IssueBook: this copy is reference-only and cannot be borrowed.', 1;

        DECLARE @PeriodDays INT;
        SELECT @PeriodDays = TRY_CAST(SettingValue AS INT)
        FROM dbo.SystemSetting
        WHERE SettingKey = N'Loan.PeriodDays';

        IF @PeriodDays IS NULL
            THROW 50021, N'sp_IssueBook: Loan.PeriodDays setting is missing.', 1;

        DECLARE @BorrowedAt DATETIME2(0) = SYSDATETIME();
        DECLARE @DueAt      DATETIME2(0) = DATEADD(DAY, @PeriodDays, @BorrowedAt);

        INSERT INTO dbo.Loan (MemberID, CopyID, IssuedByStaffID, BorrowedAt, DueAt, Status)
        VALUES (@MemberID, @CopyID, @IssuedByStaffID, @BorrowedAt, @DueAt, N'Active');

        SET @LoanID = SCOPE_IDENTITY();

        UPDATE dbo.BookCopy
        SET Status = N'On Loan'
        WHERE CopyID = @CopyID;

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0
            ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END
GO

/* =============================================================================
   Test statements — run against a freshly seeded database (01_schema.sql,
   02_seed_01_identity.sql, 02_seed_02_catalogue.sql, 02_seed_03_demo.sql).
   Each violation test is commented with the outcome it must produce.
   ============================================================================= */

-- T1: Kasun Fernando (MemberID 3, Student, no loans, no fines) borrows an
--     available, non-reference copy of Design Patterns.
-- Expected: succeeds; @NewLoanID is populated with a new LoanID.
DECLARE @NewLoanID INT;
EXEC dbo.sp_IssueBook @MemberID = 3, @CopyID = 16, @IssuedByStaffID = 1, @LoanID = @NewLoanID OUTPUT;
SELECT @NewLoanID AS T1_NewLoanID;
GO

-- T2: MemberID does not exist.
-- Expected: fails with Msg 50001.
DECLARE @NewLoanID INT;
EXEC dbo.sp_IssueBook @MemberID = 9999, @CopyID = 17, @IssuedByStaffID = 1, @LoanID = @NewLoanID OUTPUT;
GO

-- T3 setup: temporarily suspend MemberID 6's membership.
-- sp_IssueBook sets XACT_ABORT ON for the whole session (a stored-procedure
-- SET option outlives the call), so every step below that must run
-- regardless of whether the EXEC in between throws sits in its own batch.
UPDATE dbo.Member SET MembershipStatus = N'Suspended' WHERE MemberID = 6;
GO

-- T3: membership is Suspended, not Active.
-- Expected: fails with Msg 50002.
DECLARE @NewLoanID INT;
EXEC dbo.sp_IssueBook @MemberID = 6, @CopyID = 17, @IssuedByStaffID = 1, @LoanID = @NewLoanID OUTPUT;
GO

-- T3 cleanup: restore MemberID 6's membership to Active.
UPDATE dbo.Member SET MembershipStatus = N'Active' WHERE MemberID = 6;
GO

-- T4: membership has expired (MemberID 6's ExpiryDate is in the past).
-- Expected: fails with Msg 50003.
DECLARE @NewLoanID INT;
EXEC dbo.sp_IssueBook @MemberID = 6, @CopyID = 17, @IssuedByStaffID = 1, @LoanID = @NewLoanID OUTPUT;
GO

-- T5 setup: temporarily force the Academic Staff borrowing limit to 0.
UPDATE dbo.SystemSetting SET SettingValue = N'0' WHERE SettingKey = N'Borrowing.Limit.Academic Staff';
GO

-- T5: borrowing limit reached.
-- Expected: fails with Msg 50004.
DECLARE @NewLoanID INT;
EXEC dbo.sp_IssueBook @MemberID = 1, @CopyID = 17, @IssuedByStaffID = 1, @LoanID = @NewLoanID OUTPUT;
GO

-- T5 cleanup: restore the Academic Staff borrowing limit to 10.
UPDATE dbo.SystemSetting SET SettingValue = N'10' WHERE SettingKey = N'Borrowing.Limit.Academic Staff';
GO

-- T6: MemberID 1 has an overdue loan seeded by 02_seed_03_demo.sql.
-- Expected: fails with Msg 50005.
DECLARE @NewLoanID INT;
EXEC dbo.sp_IssueBook @MemberID = 1, @CopyID = 17, @IssuedByStaffID = 1, @LoanID = @NewLoanID OUTPUT;
GO

-- T7 setup: give MemberID 2 an unpaid Overdue fine against their existing loan.
INSERT INTO dbo.Fine (MemberID, LoanID, FineType, RatePerDay, AmountAssessed, Status)
SELECT 2, LoanID, N'Overdue', 20.00, 40.00, N'Pending'
FROM dbo.Loan WHERE MemberID = 2 AND CopyID = 6;
GO

-- T7: MemberID 2 has an unpaid overdue fine but no overdue loan of their own.
-- Expected: fails with Msg 50006.
DECLARE @NewLoanID INT;
EXEC dbo.sp_IssueBook @MemberID = 2, @CopyID = 17, @IssuedByStaffID = 1, @LoanID = @NewLoanID OUTPUT;
GO

-- T8: CopyID does not exist.
-- Expected: fails with Msg 50007.
DECLARE @NewLoanID INT;
EXEC dbo.sp_IssueBook @MemberID = 3, @CopyID = 999999, @IssuedByStaffID = 1, @LoanID = @NewLoanID OUTPUT;
GO

-- T9: copy exists but is already On Loan (CopyID 1, seeded fully checked out).
-- Expected: fails with Msg 50008.
DECLARE @NewLoanID INT;
EXEC dbo.sp_IssueBook @MemberID = 3, @CopyID = 1, @IssuedByStaffID = 1, @LoanID = @NewLoanID OUTPUT;
GO

-- T10: copy is reference-only (CopyID 8, Introduction to Algorithms copy 4).
-- Expected: fails with Msg 50009.
DECLARE @NewLoanID INT;
EXEC dbo.sp_IssueBook @MemberID = 3, @CopyID = 8, @IssuedByStaffID = 1, @LoanID = @NewLoanID OUTPUT;
GO
