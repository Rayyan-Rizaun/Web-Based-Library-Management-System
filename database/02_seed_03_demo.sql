/* =============================================================================
   Library Management System — demo data (circulation & feedback)
   IT2140 Part 02 · Group MLB-B11G2-10 · Microsoft SQL Server 2022
   -----------------------------------------------------------------------------
   Run after database/02_seed_01_identity.sql and 02_seed_02_catalogue.sql.
   Uses existing Member (1, 2), StaffProfile (1) and BookCopy (5, 6, 9) rows.
   ============================================================================= */

SET NOCOUNT ON;
GO

INSERT INTO dbo.FeedbackCategory (CategoryName) VALUES
    (N'Service'),
    (N'Facilities'),
    (N'Book Request'),
    (N'Website'),
    (N'Other');
GO

-- Loan 1: Member 1, currently overdue by 5 days.
INSERT INTO dbo.Loan (MemberID, CopyID, IssuedByStaffID, BorrowedAt, DueAt, Status)
VALUES (1, 5, 1, DATEADD(DAY, -19, SYSDATETIME()), DATEADD(DAY, -5, SYSDATETIME()), N'Active');
GO

-- Loan 2: Member 2, due in 2 days.
INSERT INTO dbo.Loan (MemberID, CopyID, IssuedByStaffID, BorrowedAt, DueAt, Status)
VALUES (2, 6, 1, DATEADD(DAY, -12, SYSDATETIME()), DATEADD(DAY, 2, SYSDATETIME()), N'Active');
GO

-- Loan 3: Member 1, returned last month, 5 days late (the loan behind the Partially Paid fine below).
INSERT INTO dbo.Loan (MemberID, CopyID, IssuedByStaffID, ReturnedToStaffID, BorrowedAt, DueAt, ReturnedAt, ReturnCondition, Status)
VALUES (1, 9, 1, 1, DATEADD(DAY, -44, SYSDATETIME()), DATEADD(DAY, -30, SYSDATETIME()), DATEADD(DAY, -25, SYSDATETIME()), N'Good', N'Returned');
GO

-- Fine 1: Pending, against Loan 1 (Member 1's current overdue loan) — 5 days x LKR 20/day.
INSERT INTO dbo.Fine (MemberID, LoanID, FineType, RatePerDay, AmountAssessed, Status)
SELECT 1, LoanID, N'Overdue', 20.00, 100.00, N'Pending'
FROM dbo.Loan
WHERE MemberID = 1 AND CopyID = 5 AND ReturnedAt IS NULL;
GO

-- Fine 2: Partially Paid, against Loan 3 (Member 1's late return last month) — 5 days x LKR 20/day.
INSERT INTO dbo.Fine (MemberID, LoanID, FineType, RatePerDay, AmountAssessed, Status)
SELECT 1, LoanID, N'Overdue', 20.00, 100.00, N'Partially Paid'
FROM dbo.Loan
WHERE MemberID = 1 AND CopyID = 9 AND ReturnedAt IS NOT NULL;
GO
