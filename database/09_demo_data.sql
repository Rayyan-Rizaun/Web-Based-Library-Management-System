SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
GO

DECLARE @LoanC INT;
INSERT INTO dbo.Loan (MemberID, CopyID, IssuedByStaffID, ReturnedToStaffID, BorrowedAt, DueAt, ReturnedAt, ReturnCondition, Status)
VALUES (8, 21, 3, 3, DATEADD(DAY,-175,SYSDATETIME()), DATEADD(DAY,-161,SYSDATETIME()), DATEADD(DAY,-165,SYSDATETIME()), N'Good', N'Returned');

INSERT INTO dbo.Loan (MemberID, CopyID, IssuedByStaffID, ReturnedToStaffID, BorrowedAt, DueAt, ReturnedAt, ReturnCondition, Status)
VALUES (8, 23, 5, 5, DATEADD(DAY,-135,SYSDATETIME()), DATEADD(DAY,-121,SYSDATETIME()), DATEADD(DAY,-123,SYSDATETIME()), N'Good', N'Returned');

INSERT INTO dbo.Loan (MemberID, CopyID, IssuedByStaffID, ReturnedToStaffID, BorrowedAt, DueAt, ReturnedAt, ReturnCondition, Status)
VALUES (8, 26, 2, 2, DATEADD(DAY,-95,SYSDATETIME()), DATEADD(DAY,-53,SYSDATETIME()), DATEADD(DAY,-55,SYSDATETIME()), N'Good', N'Returned');
SET @LoanC = SCOPE_IDENTITY();

INSERT INTO dbo.LoanRenewal (LoanID, RequestedByUserID, RequestedAt, ApprovedByStaffID, OldDueAt, NewDueAt, Status)
VALUES
    (@LoanC, 8, DATEADD(DAY,-82,SYSDATETIME()), 2, DATEADD(DAY,-81,SYSDATETIME()), DATEADD(DAY,-67,SYSDATETIME()), N'Approved'),
    (@LoanC, 8, DATEADD(DAY,-68,SYSDATETIME()), 2, DATEADD(DAY,-67,SYSDATETIME()), DATEADD(DAY,-53,SYSDATETIME()), N'Approved');

INSERT INTO dbo.Loan (MemberID, CopyID, IssuedByStaffID, ReturnedToStaffID, BorrowedAt, DueAt, ReturnedAt, ReturnCondition, Status)
VALUES (8, 30, 6, 6, DATEADD(DAY,-40,SYSDATETIME()), DATEADD(DAY,-26,SYSDATETIME()), DATEADD(DAY,-28,SYSDATETIME()), N'Good', N'Returned');
GO

INSERT INTO dbo.Notification (UserID, NotificationType, Title, Message, IsRead, CreatedAt) VALUES
    (8, N'DueSoon',        N'Loan due soon',            N'Your borrowed book is due back in a few days.', 1, DATEADD(DAY,-30,SYSDATETIME())),
    (8, N'RenewalDecision', N'Renewal approved',         N'Your renewal request was approved. The due date has been updated.', 1, DATEADD(DAY,-68,SYSDATETIME())),
    (8, N'ReviewDecision',  N'Your review was approved', N'Your review of "Design Patterns" is now published.', 0, DATEADD(DAY,-38,SYSDATETIME()));
GO

DECLARE @ReviewDP INT;
INSERT INTO dbo.BookReview (BookID, MemberID, Rating, ReviewText, Status, SubmittedAt, UpdatedAt)
VALUES (5, 8, 5, N'Excellent, practical examples that helped a lot with my coursework.', N'Approved', DATEADD(DAY,-42,SYSDATETIME()), DATEADD(DAY,-38,SYSDATETIME()));
SET @ReviewDP = SCOPE_IDENTITY();

INSERT INTO dbo.ReviewModerationHistory (ReviewID, PreviousStatus, NewStatus, Reason, ModeratorStaffID, ModeratedAt)
VALUES (@ReviewDP, N'Pending', N'Approved', NULL, 2, DATEADD(DAY,-38,SYSDATETIME()));
GO

DECLARE @FeedbackSearch INT;
INSERT INTO dbo.MemberFeedback (FeedbackReference, MemberID, FeedbackCategoryID, Subject, Description, Priority, Status, AdminResponse, SubmittedAt, UpdatedAt)
VALUES (N'FB-2026-100201', 8, 4, N'Search results are slow to load',
        N'The catalogue search takes a long time to return results when searching by author.',
        N'Medium', N'Resolved', N'We have optimised the search index; please let us know if the issue continues.',
        DATEADD(DAY,-25,SYSDATETIME()), DATEADD(DAY,-20,SYSDATETIME()));
SET @FeedbackSearch = SCOPE_IDENTITY();

INSERT INTO dbo.FeedbackHistory (FeedbackID, PreviousStatus, NewStatus, PreviousResponse, NewResponse, ChangedByUserID, ChangedAt)
VALUES
    (@FeedbackSearch, N'Submitted',   N'Under Review', NULL, NULL, 3, DATEADD(DAY,-24,SYSDATETIME())),
    (@FeedbackSearch, N'Under Review',N'In Progress',  NULL, NULL, 3, DATEADD(DAY,-23,SYSDATETIME())),
    (@FeedbackSearch, N'In Progress', N'Resolved', NULL,
        N'We have optimised the search index; please let us know if the issue continues.', 3, DATEADD(DAY,-20,SYSDATETIME()));
GO

INSERT INTO dbo.Loan (MemberID, CopyID, IssuedByStaffID, BorrowedAt, DueAt, Status)
VALUES (9, 22, 3, DATEADD(DAY,-22,SYSDATETIME()), DATEADD(DAY,-8,SYSDATETIME()), N'Active');

INSERT INTO dbo.Fine (MemberID, LoanID, FineType, RatePerDay, AmountAssessed, Status)
SELECT 9, LoanID, N'Overdue', 20.00, 160.00, N'Pending'
FROM dbo.Loan WHERE MemberID = 9 AND CopyID = 22 AND ReturnedAt IS NULL;
GO

DECLARE @LoanLate INT, @FineLate INT;
INSERT INTO dbo.Loan (MemberID, CopyID, IssuedByStaffID, ReturnedToStaffID, BorrowedAt, DueAt, ReturnedAt, ReturnCondition, Status)
VALUES (9, 24, 5, 5, DATEADD(DAY,-60,SYSDATETIME()), DATEADD(DAY,-46,SYSDATETIME()), DATEADD(DAY,-41,SYSDATETIME()), N'Fair', N'Returned');
SET @LoanLate = SCOPE_IDENTITY();

INSERT INTO dbo.Fine (MemberID, LoanID, FineType, RatePerDay, AmountAssessed, AssessedAt, Status)
VALUES (9, @LoanLate, N'Overdue', 20.00, 100.00, DATEADD(DAY,-41,SYSDATETIME()), N'Partially Paid');
SET @FineLate = SCOPE_IDENTITY();

INSERT INTO dbo.FinePayment (FineID, ReceiptNumber, AmountPaid, PaymentMethod, PaymentStatus, PaidAt, ReceivedByStaffID)
VALUES (@FineLate, N'RCT-2026-100301', 60.00, N'Cash', N'Completed', DATEADD(DAY,-40,SYSDATETIME()), 4);

INSERT INTO dbo.AuditLog (UserID, ActionName, EntityName, EntityID, OccurredAt)
VALUES (4, N'PAYMENT', N'Fine', CAST(@FineLate AS NVARCHAR(20)), DATEADD(DAY,-40,SYSDATETIME()));
GO

DECLARE @LoanLost INT, @IncidentLost INT, @FineLost INT;
INSERT INTO dbo.Loan (MemberID, CopyID, IssuedByStaffID, BorrowedAt, DueAt, Status)
VALUES (9, 11, 2, DATEADD(DAY,-35,SYSDATETIME()), DATEADD(DAY,-21,SYSDATETIME()), N'Lost');
SET @LoanLost = SCOPE_IDENTITY();

INSERT INTO dbo.BookIncident (CopyID, LoanID, MemberID, RecordedByStaffID, IncidentType, Description, ReportedAt, Status)
VALUES (11, @LoanLost, 9, 2, N'Lost', N'Member reported the copy lost; unable to return after the due date.', DATEADD(DAY,-18,SYSDATETIME()), N'Charged');
SET @IncidentLost = SCOPE_IDENTITY();

INSERT INTO dbo.Fine (MemberID, IncidentID, FineType, AmountAssessed, AssessedAt, Status)
VALUES (9, @IncidentLost, N'Lost', 7400.00, DATEADD(DAY,-18,SYSDATETIME()), N'Pending');
SET @FineLost = SCOPE_IDENTITY();

INSERT INTO dbo.FineAppeal (FineID, AppealReason, SubmittedAt, Status)
VALUES (@FineLost, N'I returned the book to the drop box before the due date; it may have been misplaced by staff.',
        DATEADD(DAY,-15,SYSDATETIME()), N'Pending');
GO

DECLARE @LoanOld INT, @FineWaived INT;
INSERT INTO dbo.Loan (MemberID, CopyID, IssuedByStaffID, ReturnedToStaffID, BorrowedAt, DueAt, ReturnedAt, ReturnCondition, Status)
VALUES (9, 27, 6, 6, DATEADD(DAY,-200,SYSDATETIME()), DATEADD(DAY,-186,SYSDATETIME()), DATEADD(DAY,-180,SYSDATETIME()), N'Good', N'Returned');
SET @LoanOld = SCOPE_IDENTITY();

INSERT INTO dbo.Fine (MemberID, LoanID, FineType, RatePerDay, AmountAssessed, AssessedAt, Status, WaiverReason, WaivedByStaffID)
VALUES (9, @LoanOld, N'Overdue', 20.00, 120.00, DATEADD(DAY,-180,SYSDATETIME()), N'Waived',
        N'Member showed proof of a family emergency; fine waived as a goodwill gesture.', 1);
SET @FineWaived = SCOPE_IDENTITY();

INSERT INTO dbo.AuditLog (UserID, ActionName, EntityName, EntityID, OccurredAt)
VALUES (1, N'WAIVE', N'Fine', CAST(@FineWaived AS NVARCHAR(20)), DATEADD(DAY,-179,SYSDATETIME()));
GO

INSERT INTO dbo.Reservation (BookID, MemberID, RequestedAt, Status, ReadyAt, ExpiresAt)
VALUES (8, 9, DATEADD(DAY,-10,SYSDATETIME()), N'Ready', DATEADD(DAY,-1,SYSDATETIME()), DATEADD(DAY,2,SYSDATETIME()));
GO

DECLARE @ReviewHidden INT;
INSERT INTO dbo.BookReview (BookID, MemberID, Rating, ReviewText, Status, SubmittedAt, UpdatedAt)
VALUES (6, 9, 2, N'Outdated statistics and some factual errors in chapter 4.', N'Hidden', DATEADD(DAY,-20,SYSDATETIME()), DATEADD(DAY,-5,SYSDATETIME()));
SET @ReviewHidden = SCOPE_IDENTITY();

INSERT INTO dbo.ReviewFlag (ReviewID, ReportedByMemberID, Reason, Status, ReportedAt)
VALUES (@ReviewHidden, 3, N'Contains outdated and possibly inaccurate information.', N'Open', DATEADD(DAY,-5,SYSDATETIME()));

INSERT INTO dbo.ReviewModerationHistory (ReviewID, PreviousStatus, NewStatus, Reason, ModeratorStaffID, ModeratedAt)
VALUES (@ReviewHidden, N'Approved', N'Hidden', NULL, NULL, DATEADD(DAY,-5,SYSDATETIME()));
GO

DECLARE @FeedbackDesk INT;
INSERT INTO dbo.MemberFeedback (FeedbackReference, MemberID, FeedbackCategoryID, Subject, Description, Priority, Status, AdminResponse, SubmittedAt, UpdatedAt)
VALUES (N'FB-2026-100302', 9, 1, N'Long wait time at the circulation desk',
        N'During peak hours the queue at the circulation desk can take over 20 minutes.',
        N'High', N'In Progress', N'We are reviewing staffing levels during peak hours.',
        DATEADD(DAY,-12,SYSDATETIME()), DATEADD(DAY,-9,SYSDATETIME()));
SET @FeedbackDesk = SCOPE_IDENTITY();

INSERT INTO dbo.FeedbackHistory (FeedbackID, PreviousStatus, NewStatus, PreviousResponse, NewResponse, ChangedByUserID, ChangedAt)
VALUES
    (@FeedbackDesk, N'Submitted',    N'Under Review', NULL, NULL, 3, DATEADD(DAY,-11,SYSDATETIME())),
    (@FeedbackDesk, N'Under Review', N'In Progress',  NULL, N'We are reviewing staffing levels during peak hours.', 3, DATEADD(DAY,-9,SYSDATETIME()));
GO

INSERT INTO dbo.Notification (UserID, NotificationType, Title, Message, IsRead, CreatedAt) VALUES
    (9, N'Overdue',          N'Your loan is overdue',                 N'Your loan of "Principles of Marketing" is now overdue. Please return it as soon as possible.', 0, DATEADD(DAY,-1,SYSDATETIME())),
    (9, N'FineIssued',       N'A fine has been added to your account', N'An overdue fine of LKR 100.00 has been added to your account.', 0, DATEADD(DAY,-41,SYSDATETIME())),
    (9, N'ReservationReady', N'Reservation ready for collection',      N'"Strength of Materials" is being held for you until it expires.', 1, DATEADD(DAY,-1,SYSDATETIME())),
    (9, N'FeedbackResponse', N'We responded to your feedback',         N'Library staff responded to your feedback about circulation desk wait times.', 1, DATEADD(DAY,-9,SYSDATETIME()));
GO

INSERT INTO dbo.Loan (MemberID, CopyID, IssuedByStaffID, BorrowedAt, DueAt, Status)
VALUES (2, 13, 3, DATEADD(DAY,-20,SYSDATETIME()), DATEADD(DAY,-6,SYSDATETIME()), N'Active');
INSERT INTO dbo.Fine (MemberID, LoanID, FineType, RatePerDay, AmountAssessed, Status)
SELECT 2, LoanID, N'Overdue', 20.00, 120.00, N'Pending' FROM dbo.Loan WHERE MemberID = 2 AND CopyID = 13 AND ReturnedAt IS NULL;

INSERT INTO dbo.Loan (MemberID, CopyID, IssuedByStaffID, BorrowedAt, DueAt, Status)
VALUES (4, 19, 5, DATEADD(DAY,-25,SYSDATETIME()), DATEADD(DAY,-11,SYSDATETIME()), N'Active');
INSERT INTO dbo.Fine (MemberID, LoanID, FineType, RatePerDay, AmountAssessed, Status)
SELECT 4, LoanID, N'Overdue', 20.00, 220.00, N'Pending' FROM dbo.Loan WHERE MemberID = 4 AND CopyID = 19 AND ReturnedAt IS NULL;

INSERT INTO dbo.Loan (MemberID, CopyID, IssuedByStaffID, BorrowedAt, DueAt, Status)
VALUES (6, 28, 6, DATEADD(DAY,-18,SYSDATETIME()), DATEADD(DAY,-4,SYSDATETIME()), N'Active');
INSERT INTO dbo.Fine (MemberID, LoanID, FineType, RatePerDay, AmountAssessed, Status)
SELECT 6, LoanID, N'Overdue', 20.00, 80.00, N'Pending' FROM dbo.Loan WHERE MemberID = 6 AND CopyID = 28 AND ReturnedAt IS NULL;
GO

INSERT INTO dbo.Loan (MemberID, CopyID, IssuedByStaffID, ReturnedToStaffID, BorrowedAt, DueAt, ReturnedAt, ReturnCondition, Status) VALUES
    (1, 10, 3, 3, DATEADD(DAY,-230,SYSDATETIME()), DATEADD(DAY,-216,SYSDATETIME()), DATEADD(DAY,-218,SYSDATETIME()), N'Good', N'Returned'),
    (1, 14, 2, 2, DATEADD(DAY,-150,SYSDATETIME()), DATEADD(DAY,-136,SYSDATETIME()), DATEADD(DAY,-140,SYSDATETIME()), N'Good', N'Returned'),
    (2, 20, 5, 5, DATEADD(DAY,-210,SYSDATETIME()), DATEADD(DAY,-196,SYSDATETIME()), DATEADD(DAY,-198,SYSDATETIME()), N'Good', N'Returned'),
    (2, 25, 3, 3, DATEADD(DAY,-100,SYSDATETIME()), DATEADD(DAY,-86,SYSDATETIME()),  DATEADD(DAY,-88,SYSDATETIME()),  N'Good', N'Returned'),
    (3, 29, 2, 2, DATEADD(DAY,-190,SYSDATETIME()), DATEADD(DAY,-176,SYSDATETIME()), DATEADD(DAY,-178,SYSDATETIME()), N'Fair', N'Returned'),
    (3, 31, 6, 6, DATEADD(DAY,-70,SYSDATETIME()),  DATEADD(DAY,-56,SYSDATETIME()),  DATEADD(DAY,-58,SYSDATETIME()),  N'Good', N'Returned'),
    (4, 34, 5, 5, DATEADD(DAY,-50,SYSDATETIME()),  DATEADD(DAY,-36,SYSDATETIME()),  DATEADD(DAY,-38,SYSDATETIME()),  N'Good', N'Returned'),
    (5, 36, 3, 3, DATEADD(DAY,-180,SYSDATETIME()), DATEADD(DAY,-166,SYSDATETIME()), DATEADD(DAY,-168,SYSDATETIME()), N'Good', N'Returned'),
    (5, 37, 6, 6, DATEADD(DAY,-80,SYSDATETIME()),  DATEADD(DAY,-66,SYSDATETIME()),  DATEADD(DAY,-68,SYSDATETIME()),  N'Good', N'Returned'),
    (6, 40, 2, 2, DATEADD(DAY,-110,SYSDATETIME()), DATEADD(DAY,-96,SYSDATETIME()),  DATEADD(DAY,-98,SYSDATETIME()),  N'Good', N'Returned');

INSERT INTO dbo.Loan (MemberID, CopyID, IssuedByStaffID, BorrowedAt, DueAt, Status)
VALUES (7, 41, 3, DATEADD(DAY,-2,SYSDATETIME()), DATEADD(DAY,12,SYSDATETIME()), N'Active');
GO

DECLARE @LoanR7 INT, @FineR7 INT;
INSERT INTO dbo.Loan (MemberID, CopyID, IssuedByStaffID, ReturnedToStaffID, BorrowedAt, DueAt, ReturnedAt, ReturnCondition, Status)
VALUES (4, 33, 2, 2, DATEADD(DAY,-160,SYSDATETIME()), DATEADD(DAY,-146,SYSDATETIME()), DATEADD(DAY,-142,SYSDATETIME()), N'Good', N'Returned');
SET @LoanR7 = SCOPE_IDENTITY();

INSERT INTO dbo.Fine (MemberID, LoanID, FineType, RatePerDay, AmountAssessed, AssessedAt, Status)
VALUES (4, @LoanR7, N'Overdue', 20.00, 80.00, DATEADD(DAY,-142,SYSDATETIME()), N'Fully Paid');
SET @FineR7 = SCOPE_IDENTITY();

INSERT INTO dbo.FinePayment (FineID, ReceiptNumber, AmountPaid, PaymentMethod, PaymentStatus, PaidAt)
VALUES (@FineR7, N'RCT-2026-100601', 80.00, N'Card', N'Completed', DATEADD(DAY,-141,SYSDATETIME()));
GO

INSERT INTO dbo.Reservation (BookID, MemberID, RequestedAt, Status) VALUES
    (2, 3, DATEADD(DAY,-9,SYSDATETIME()), N'Waiting'),
    (2, 5, DATEADD(DAY,-6,SYSDATETIME()), N'Waiting'),
    (2, 6, DATEADD(DAY,-3,SYSDATETIME()), N'Waiting');
GO

INSERT INTO dbo.BookReview (BookID, MemberID, Rating, ReviewText, Status, SubmittedAt, UpdatedAt) VALUES
    (3,  1, 4, N'Solid coverage of the software development lifecycle.', N'Approved', DATEADD(DAY,-20,SYSDATETIME()), DATEADD(DAY,-20,SYSDATETIME())),
    (4,  1, 5, N'A short, powerful read — recommend it to every student.', N'Approved', DATEADD(DAY,-130,SYSDATETIME()), DATEADD(DAY,-130,SYSDATETIME())),
    (6,  2, 3, N'Decent introduction but some chapters feel dated.', N'Approved', DATEADD(DAY,-185,SYSDATETIME()), DATEADD(DAY,-185,SYSDATETIME())),
    (7,  2, 4, N'Comprehensive and well organised.', N'Approved', DATEADD(DAY,-80,SYSDATETIME()), DATEADD(DAY,-80,SYSDATETIME())),
    (8,  3, 5, N'Clear explanations with good worked examples.', N'Approved', DATEADD(DAY,-170,SYSDATETIME()), DATEADD(DAY,-170,SYSDATETIME())),
    (9,  3, 2, N'Too theoretical for a first read on the subject.', N'Approved', DATEADD(DAY,-50,SYSDATETIME()), DATEADD(DAY,-50,SYSDATETIME())),
    (6,  4, 4, N'Useful case studies from local businesses.', N'Approved', DATEADD(DAY,-15,SYSDATETIME()), DATEADD(DAY,-15,SYSDATETIME())),
    (10, 4, 5, N'An essential reference for the course.', N'Approved', DATEADD(DAY,-30,SYSDATETIME()), DATEADD(DAY,-30,SYSDATETIME()));
GO

DECLARE @F1 INT, @F2 INT, @F3 INT, @F4 INT, @F5 INT;

INSERT INTO dbo.MemberFeedback (FeedbackReference, MemberID, FeedbackCategoryID, Subject, Description, Priority, Status, SubmittedAt, UpdatedAt)
VALUES (N'FB-2026-100401', 1, 1, N'Printer in the reading room is out of order',
        N'The printer near the reading room has been out of order for a few days.', N'Low', N'Submitted',
        DATEADD(DAY,-3,SYSDATETIME()), DATEADD(DAY,-3,SYSDATETIME()));
SET @F1 = SCOPE_IDENTITY();

INSERT INTO dbo.MemberFeedback (FeedbackReference, MemberID, FeedbackCategoryID, Subject, Description, Priority, Status, SubmittedAt, UpdatedAt)
VALUES (N'FB-2026-100402', 2, 2, N'Air conditioning too cold on the second floor',
        N'The second floor reading area is uncomfortably cold in the afternoons.', N'Low', N'Under Review',
        DATEADD(DAY,-7,SYSDATETIME()), DATEADD(DAY,-6,SYSDATETIME()));
SET @F2 = SCOPE_IDENTITY();
INSERT INTO dbo.FeedbackHistory (FeedbackID, PreviousStatus, NewStatus, ChangedByUserID, ChangedAt)
VALUES (@F2, N'Submitted', N'Under Review', 5, DATEADD(DAY,-6,SYSDATETIME()));

INSERT INTO dbo.MemberFeedback (FeedbackReference, MemberID, FeedbackCategoryID, Subject, Description, Priority, Status, AdminResponse, SubmittedAt, UpdatedAt)
VALUES (N'FB-2026-100403', 3, 3, N'Please add more copies of Constitutional Law of Ceylon',
        N'This title is always fully borrowed during the exam period.', N'Medium', N'In Progress',
        N'We have ordered two additional copies.', DATEADD(DAY,-14,SYSDATETIME()), DATEADD(DAY,-10,SYSDATETIME()));
SET @F3 = SCOPE_IDENTITY();
INSERT INTO dbo.FeedbackHistory (FeedbackID, PreviousStatus, NewStatus, PreviousResponse, NewResponse, ChangedByUserID, ChangedAt) VALUES
    (@F3, N'Submitted',    N'Under Review', NULL, NULL, 3, DATEADD(DAY,-13,SYSDATETIME())),
    (@F3, N'Under Review', N'In Progress',  NULL, N'We have ordered two additional copies.', 3, DATEADD(DAY,-10,SYSDATETIME()));

INSERT INTO dbo.MemberFeedback (FeedbackReference, MemberID, FeedbackCategoryID, Subject, Description, Priority, Status, AdminResponse, SubmittedAt, UpdatedAt)
VALUES (N'FB-2026-100404', 5, 4, N'Cannot reset password from the mobile site',
        N'The reset-password form does not submit correctly on a mobile browser.', N'High', N'Resolved',
        N'Fixed a mobile layout bug affecting the reset form.', DATEADD(DAY,-30,SYSDATETIME()), DATEADD(DAY,-25,SYSDATETIME()));
SET @F4 = SCOPE_IDENTITY();
INSERT INTO dbo.FeedbackHistory (FeedbackID, PreviousStatus, NewStatus, PreviousResponse, NewResponse, ChangedByUserID, ChangedAt) VALUES
    (@F4, N'Submitted',    N'Under Review', NULL, NULL, 2, DATEADD(DAY,-29,SYSDATETIME())),
    (@F4, N'Under Review', N'In Progress',  NULL, NULL, 2, DATEADD(DAY,-27,SYSDATETIME())),
    (@F4, N'In Progress',  N'Resolved',     NULL, N'Fixed a mobile layout bug affecting the reset form.', 2, DATEADD(DAY,-25,SYSDATETIME()));

INSERT INTO dbo.MemberFeedback (FeedbackReference, MemberID, FeedbackCategoryID, Subject, Description, Priority, Status, AdminResponse, SubmittedAt, UpdatedAt)
VALUES (N'FB-2026-100405', 6, 5, N'Suggestion box for extended hours during exams',
        N'Consider extending opening hours during the final examination period.', N'Low', N'Closed',
        N'Thank you for the suggestion; forwarded to the administration.', DATEADD(DAY,-60,SYSDATETIME()), DATEADD(DAY,-55,SYSDATETIME()));
SET @F5 = SCOPE_IDENTITY();
INSERT INTO dbo.FeedbackHistory (FeedbackID, PreviousStatus, NewStatus, PreviousResponse, NewResponse, ChangedByUserID, ChangedAt) VALUES
    (@F5, N'Submitted',    N'Under Review', NULL, NULL, 6, DATEADD(DAY,-59,SYSDATETIME())),
    (@F5, N'Under Review', N'In Progress',  NULL, NULL, 6, DATEADD(DAY,-58,SYSDATETIME())),
    (@F5, N'In Progress',  N'Resolved', NULL, N'Thank you for the suggestion; forwarded to the administration.', 6, DATEADD(DAY,-56,SYSDATETIME())),
    (@F5, N'Resolved',     N'Closed', NULL, NULL, 6, DATEADD(DAY,-55,SYSDATETIME()));
GO

INSERT INTO dbo.AuditLog (UserID, ActionName, EntityName, EntityID, OccurredAt) VALUES
    (1, N'LOGIN',   N'AppUser', N'1',  DATEADD(HOUR,-1,SYSDATETIME())),
    (2, N'LOGIN',   N'AppUser', N'2',  DATEADD(HOUR,-2,SYSDATETIME())),
    (3, N'LOGIN',   N'AppUser', N'3',  DATEADD(DAY,-1,SYSDATETIME())),
    (4, N'LOGIN',   N'AppUser', N'4',  DATEADD(DAY,-2,SYSDATETIME())),
    (8, N'LOGIN',   N'AppUser', N'8',  DATEADD(HOUR,-3,SYSDATETIME())),
    (9, N'LOGIN',   N'AppUser', N'9',  DATEADD(HOUR,-5,SYSDATETIME())),
    (2, N'UPDATE',  N'Book', N'1',  DATEADD(DAY,-40,SYSDATETIME())),
    (3, N'UPDATE',  N'Book', N'6',  DATEADD(DAY,-35,SYSDATETIME())),
    (5, N'UPDATE',  N'Book', N'9',  DATEADD(DAY,-20,SYSDATETIME())),
    (2, N'UPDATE',  N'Book', N'12', DATEADD(DAY,-15,SYSDATETIME())),
    (5, N'UPDATE',  N'Book', N'3',  DATEADD(DAY,-10,SYSDATETIME())),
    (4, N'WAIVE',   N'Fine', N'5',  DATEADD(DAY,-30,SYSDATETIME())),
    (1, N'WAIVE',   N'Fine', N'8',  DATEADD(DAY,-25,SYSDATETIME())),
    (1, N'APPROVE', N'FineAppeal', N'1', DATEADD(DAY,-60,SYSDATETIME())),
    (4, N'REJECT',  N'FineAppeal', N'2', DATEADD(DAY,-55,SYSDATETIME())),
    (1, N'APPROVE', N'FineAppeal', N'3', DATEADD(DAY,-50,SYSDATETIME())),
    (1, N'APPROVE', N'Member', N'7',  DATEADD(HOUR,-1,SYSDATETIME())),
    (1, N'REJECT',  N'Member', N'10', DATEADD(HOUR,-2,SYSDATETIME()));
GO

INSERT INTO dbo.FailedLoginAttempt (EmailTried, IPAddress, FailureReason, AttemptedAt) VALUES
    (N'unknown.person@example.com',    N'102.89.23.14', N'UnknownEmail',        DATEADD(DAY,-6,SYSDATETIME())),
    (N'nouser@nlms.lk',                N'41.202.12.9',  N'UnknownEmail',        DATEADD(DAY,-4,SYSDATETIME())),
    (N'user1@nlms.lk',                 N'192.168.1.45', N'BadPassword',         DATEADD(DAY,-2,SYSDATETIME())),
    (N'user2@nlms.lk',                 N'192.168.1.46', N'BadPassword',         DATEADD(DAY,-1,SYSDATETIME())),
    (N'kasun.fernando@nlms.lk',        N'10.0.0.23',    N'AccountLocked',       DATEADD(HOUR,-12,SYSDATETIME())),
    (N'sanduni.jayawardena@nlms.lk',   N'10.0.0.24',    N'AccountDeactivated',  DATEADD(HOUR,-10,SYSDATETIME()));
GO

INSERT INTO dbo.ReportAudit (RequestedByUserID, ReportType, FilterJson, GeneratedAt) VALUES
    (1, N'BorrowedBooks', N'{"from":"2026-03-01","to":"2026-09-17"}', DATEADD(DAY,-5,SYSDATETIME())),
    (1, N'OverdueBooks',  N'{"from":"2026-08-01","to":"2026-09-17"}', DATEADD(DAY,-3,SYSDATETIME())),
    (4, N'FineCollection',N'{"from":"2026-01-01","to":"2026-09-17"}', DATEADD(DAY,-2,SYSDATETIME())),
    (2, N'Reviews',       NULL, DATEADD(DAY,-1,SYSDATETIME()));
GO

INSERT INTO dbo.DatabaseBackupLog (BackupType, BackupPath, StartedAt, CompletedAt, Status, InitiatedByUserID, ErrorMessage)
VALUES (N'Full', N'D:\Backups\LibraryDB_Full_20260910.bak',
        DATEADD(DAY,-7,SYSDATETIME()), DATEADD(MINUTE,15,DATEADD(DAY,-7,SYSDATETIME())), N'Succeeded', 1, NULL);

INSERT INTO dbo.DatabaseBackupLog (BackupType, BackupPath, StartedAt, CompletedAt, Status, InitiatedByUserID, ErrorMessage)
VALUES (N'Differential', N'D:\Backups\LibraryDB_Diff_20260914.bak',
        DATEADD(DAY,-3,SYSDATETIME()), DATEADD(MINUTE,5,DATEADD(DAY,-3,SYSDATETIME())), N'Failed', NULL, N'Insufficient disk space on backup volume D:.');
GO
