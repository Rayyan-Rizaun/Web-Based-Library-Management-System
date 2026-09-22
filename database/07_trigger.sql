/* =============================================================================
   trg_Loan_AfterReturn — UC-03 return flow. Fires only for rows whose
   ReturnedAt genuinely transitions from NULL to a value in this statement
   (a later correction to an already-returned row does not re-fire it).

   Joins inserted to deleted on LoanID into a table variable first, so every
   step below (the BookCopy update, the Fine insert, the AuditLog insert) is
   one set-based statement over every returned row in the batch — never a
   scalar @LoanID/@MemberID assignment, which would silently keep only one
   arbitrary row on a multi-row UPDATE.
   ============================================================================= */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

CREATE OR ALTER TRIGGER dbo.trg_Loan_AfterReturn
ON dbo.Loan
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF NOT UPDATE(ReturnedAt)
        RETURN;

    DECLARE @Returned TABLE (
        LoanID     INT          NOT NULL PRIMARY KEY,
        MemberID   INT          NOT NULL,
        CopyID     INT          NOT NULL,
        DueAt      DATETIME2(0) NOT NULL,
        ReturnedAt DATETIME2(0) NOT NULL
    );

    INSERT INTO @Returned (LoanID, MemberID, CopyID, DueAt, ReturnedAt)
    SELECT i.LoanID, i.MemberID, i.CopyID, i.DueAt, i.ReturnedAt
    FROM inserted i
    INNER JOIN deleted d ON d.LoanID = i.LoanID
    WHERE d.ReturnedAt IS NULL AND i.ReturnedAt IS NOT NULL;

    IF NOT EXISTS (SELECT 1 FROM @Returned)
        RETURN;

    -- Every returned copy, across every row in the batch, goes back on the shelf.
    UPDATE c
    SET c.Status = N'Available'
    FROM dbo.BookCopy c
    INNER JOIN @Returned r ON r.CopyID = c.CopyID;

    DECLARE @RatePerDay DECIMAL(10,2), @Cap DECIMAL(10,2);
    SELECT @RatePerDay = TRY_CAST(SettingValue AS DECIMAL(18,4)) FROM dbo.SystemSetting WHERE SettingKey = N'Fine.RatePerDay';
    SELECT @Cap         = TRY_CAST(SettingValue AS DECIMAL(18,4)) FROM dbo.SystemSetting WHERE SettingKey = N'Fine.MaxPerLoan';

    -- One Pending Overdue fine per late loan in the batch, capped per loan.
    INSERT INTO dbo.Fine (MemberID, LoanID, FineType, RatePerDay, AmountAssessed, Status)
    SELECT
        r.MemberID,
        r.LoanID,
        N'Overdue',
        @RatePerDay,
        CAST(
            CASE
                WHEN DATEDIFF(DAY, r.DueAt, r.ReturnedAt) * @RatePerDay > @Cap THEN @Cap
                ELSE DATEDIFF(DAY, r.DueAt, r.ReturnedAt) * @RatePerDay
            END AS DECIMAL(10,2)
        ),
        N'Pending'
    FROM @Returned r
    WHERE r.ReturnedAt > r.DueAt;

    -- One AuditLog row per returned loan in the batch.
    INSERT INTO dbo.AuditLog (UserID, ActionName, EntityName, EntityID, Details)
    SELECT
        NULL,
        N'RETURN',
        N'Loan',
        CAST(r.LoanID AS NVARCHAR(100)),
        CONCAT(
            N'{"loanId":', r.LoanID,
            N',"memberId":', r.MemberID,
            N',"copyId":', r.CopyID,
            N',"returnedAt":"', CONVERT(NVARCHAR(19), r.ReturnedAt, 126), N'"}'
        )
    FROM @Returned r;
END
GO

/* =============================================================================
   Test statements — run after database/06_procedure.sql's own test block, on
   the same freshly seeded database. Each test is commented with the outcome
   it must produce.
   ============================================================================= */

-- T1: return two open loans in a single multi-row UPDATE — Member 1's
--     overdue loan on Copy 5, and Member 2's not-yet-due loan on Copy 6.
-- Expected: both copies become Available; exactly one Pending Overdue fine
--     is inserted, for the overdue Copy-5 loan only (days late x LKR 20/day,
--     capped at LKR 500); two RETURN rows are written to AuditLog, one per loan.
UPDATE dbo.Loan
SET ReturnedAt = SYSDATETIME(), ReturnedToStaffID = 1, ReturnCondition = N'Good', Status = N'Returned'
WHERE ReturnedAt IS NULL
  AND ((MemberID = 1 AND CopyID = 5) OR (MemberID = 2 AND CopyID = 6));

SELECT CopyID, Status FROM dbo.BookCopy WHERE CopyID IN (5, 6);

SELECT f.MemberID, f.LoanID, f.FineType, f.AmountAssessed, f.Status
FROM dbo.Fine f
INNER JOIN dbo.Loan l ON l.LoanID = f.LoanID
WHERE l.CopyID IN (5, 6);

SELECT ActionName, EntityName, EntityID
FROM dbo.AuditLog
WHERE ActionName = N'RETURN'
ORDER BY AuditID DESC;
GO

-- T2: editing an already-returned loan without touching ReturnedAt's
--     NULL -> value transition must not fire the trigger again.
-- Expected: the fine count for Copy 5's loan stays at exactly 1.
UPDATE dbo.Loan
SET ReturnCondition = N'Good'
WHERE CopyID = 5 AND ReturnedAt IS NOT NULL;

SELECT COUNT(*) AS FineRowsForCopy5Loan
FROM dbo.Fine f
INNER JOIN dbo.Loan l ON l.LoanID = f.LoanID
WHERE l.CopyID = 5;
GO
