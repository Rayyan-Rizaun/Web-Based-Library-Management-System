-- Q1: Which book copies are currently available to borrow, ordered by shelf location?
SELECT CopyID, AccessionNumber, Barcode, ShelfLocation, CopyCondition, Status
FROM dbo.BookCopy
WHERE Status = N'Available'
ORDER BY ShelfLocation;
GO

-- Q2: Which copies are currently Damaged or Lost, and which book does each belong to?
SELECT c.CopyID, c.AccessionNumber, c.Status, b.Title
FROM dbo.BookCopy c
INNER JOIN dbo.Book b ON b.BookID = c.BookID
WHERE c.Status IN (N'Damaged', N'Lost');
GO

-- Q3: For every active loan, who has the book and when is it due?
SELECT m.MemberID, u.FirstName + N' ' + u.LastName AS MemberName, b.Title, l.DueAt
FROM dbo.Loan l
INNER JOIN dbo.Member m ON m.MemberID = l.MemberID
INNER JOIN dbo.AppUser u ON u.UserID = m.UserID
INNER JOIN dbo.BookCopy c ON c.CopyID = l.CopyID
INNER JOIN dbo.Book b ON b.BookID = c.BookID
WHERE l.Status = N'Active'
ORDER BY l.DueAt;
GO

-- Q4: Which books have never received a review?
SELECT b.BookID, b.Title
FROM dbo.Book b
LEFT JOIN dbo.BookReview r ON r.BookID = b.BookID
WHERE r.ReviewID IS NULL
ORDER BY b.Title;
GO

-- Q5: Which people hold both a staff position and a library membership?
SELECT u.UserID, u.FirstName + N' ' + u.LastName AS FullName, sp.JobTitle, m.MembershipNo
FROM dbo.AppUser u
INNER JOIN dbo.StaffProfile sp ON sp.UserID = u.UserID
INNER JOIN dbo.Member m ON m.UserID = u.UserID;
GO

-- Q6: Which catalogue categories classify more than one book?
SELECT c.CategoryName, COUNT(*) AS BookCount
FROM dbo.BookCategory bc
INNER JOIN dbo.Category c ON c.CategoryID = bc.CategoryID
GROUP BY c.CategoryName
HAVING COUNT(*) > 1
ORDER BY BookCount DESC;
GO

-- Q7: Which fine(s) were assessed at the single highest amount the library has ever charged?
SELECT FineID, MemberID, FineType, AmountAssessed, AssessedAt
FROM dbo.Fine
WHERE AmountAssessed = (SELECT MAX(AmountAssessed) FROM dbo.Fine);
GO

-- Q8: For each reviewed book, which review carries that book's highest rating?
SELECT br.ReviewID, br.BookID, br.Rating, br.ReviewText
FROM dbo.BookReview br
WHERE br.Rating = (SELECT MAX(br2.Rating) FROM dbo.BookReview br2 WHERE br2.BookID = br.BookID)
ORDER BY br.BookID;
GO

-- Q9: Which members have paid more than LKR 100 in total toward their fines?
SELECT t.MemberID, u.FirstName + N' ' + u.LastName AS MemberName, t.TotalPaid
FROM (
    SELECT f.MemberID, SUM(p.AmountPaid) AS TotalPaid
    FROM dbo.Fine f
    INNER JOIN dbo.FinePayment p ON p.FineID = f.FineID AND p.PaymentStatus = N'Completed'
    GROUP BY f.MemberID
) t
INNER JOIN dbo.Member m ON m.MemberID = t.MemberID
INNER JOIN dbo.AppUser u ON u.UserID = m.UserID
WHERE t.TotalPaid > 100
ORDER BY t.TotalPaid DESC;
GO

-- Q10: Which members currently hold an active reservation (Waiting or Ready)?
SELECT DISTINCT m.MemberID, u.FirstName + N' ' + u.LastName AS MemberName
FROM dbo.Member m
INNER JOIN dbo.AppUser u ON u.UserID = m.UserID
WHERE EXISTS (
    SELECT 1 FROM dbo.Reservation r WHERE r.MemberID = m.MemberID AND r.Status IN (N'Waiting', N'Ready')
);
GO

-- Q11: Which books currently have no available copy for a member to borrow?
SELECT b.BookID, b.Title
FROM dbo.Book b
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.BookCopy c WHERE c.BookID = b.BookID AND c.Status = N'Available' AND c.IsReferenceOnly = 0
);
GO

-- Q12: Which books are classified under Computer Science or Software Engineering?
SELECT DISTINCT b.BookID, b.Title
FROM dbo.Book b
WHERE b.BookID IN (
    SELECT bc.BookID FROM dbo.BookCategory bc INNER JOIN dbo.Category c ON c.CategoryID = bc.CategoryID
    WHERE c.CategoryName IN (N'Computer Science', N'Software Engineering')
)
ORDER BY b.Title;
GO

-- Q13: Which members have never borrowed a book?
SELECT m.MemberID, u.FirstName + N' ' + u.LastName AS MemberName
FROM dbo.Member m
INNER JOIN dbo.AppUser u ON u.UserID = m.UserID
WHERE m.MemberID NOT IN (SELECT MemberID FROM dbo.Loan);
GO

-- Q14: Who holds the Librarian role or the Finance Officer role?
SELECT DISTINCT u.FirstName + N' ' + u.LastName AS FullName, N'Librarian' AS RoleName
FROM dbo.AppUser u
INNER JOIN dbo.UserRole ur ON ur.UserID = u.UserID
INNER JOIN dbo.Role r ON r.RoleID = ur.RoleID
WHERE r.RoleName = N'Librarian'
UNION
SELECT DISTINCT u.FirstName + N' ' + u.LastName, N'Finance Officer'
FROM dbo.AppUser u
INNER JOIN dbo.UserRole ur ON ur.UserID = u.UserID
INNER JOIN dbo.Role r ON r.RoleID = ur.RoleID
WHERE r.RoleName = N'Finance Officer';
GO

-- Q15: For each Waiting reservation, what is the member's position in that title's queue?
SELECT r.ReservationID, b.Title, u.FirstName + N' ' + u.LastName AS MemberName, r.RequestedAt,
       ROW_NUMBER() OVER (PARTITION BY r.BookID ORDER BY r.RequestedAt) AS QueuePosition
FROM dbo.Reservation r
INNER JOIN dbo.Book b ON b.BookID = r.BookID
INNER JOIN dbo.Member m ON m.MemberID = r.MemberID
INNER JOIN dbo.AppUser u ON u.UserID = m.UserID
WHERE r.Status = N'Waiting'
ORDER BY b.Title, QueuePosition;
GO

-- Q16: How should each copy be labelled on the catalogue's availability display?
SELECT CopyID, AccessionNumber, Status, IsReferenceOnly,
    CASE
        WHEN IsReferenceOnly = 1 THEN N'Reference only'
        WHEN Status = N'Available' THEN N'Available to borrow'
        WHEN Status = N'On Loan' THEN N'Currently on loan'
        WHEN Status IN (N'Damaged', N'Lost', N'Withdrawn', N'Under Repair') THEN N'Out of circulation'
        ELSE N'On hold'
    END AS CirculationLabel
FROM dbo.BookCopy
ORDER BY CopyID;
GO

-- Q17: How many days overdue was each late loan when it was finally returned?
SELECT LoanID, MemberID, DueAt, ReturnedAt, DATEDIFF(DAY, DueAt, ReturnedAt) AS DaysLate
FROM dbo.Loan
WHERE ReturnedAt IS NOT NULL AND ReturnedAt > DueAt
ORDER BY DaysLate DESC;
GO

-- Q18: How many loans have been issued in the last 30 days?
SELECT COUNT(*) AS LoansLast30Days
FROM dbo.Loan
WHERE BorrowedAt >= DATEADD(DAY, -30, SYSDATETIME());
GO

-- Q19: What is the outstanding balance on every fine that is not yet fully paid or waived?
SELECT f.FineID, f.MemberID, f.FineType, f.AmountAssessed,
       ISNULL(SUM(p.AmountPaid), 0) AS TotalPaid,
       f.AmountAssessed - ISNULL(SUM(p.AmountPaid), 0) AS Balance
FROM dbo.Fine f
LEFT JOIN dbo.FinePayment p ON p.FineID = f.FineID AND p.PaymentStatus = N'Completed'
WHERE f.Status NOT IN (N'Fully Paid', N'Waived')
GROUP BY f.FineID, f.MemberID, f.FineType, f.AmountAssessed
ORDER BY Balance DESC;
GO

-- Q20: Which books match a catalogue search for "design" in the title or description?
SELECT BookID, Title, Description
FROM dbo.Book
WHERE Title LIKE N'%design%' OR Description LIKE N'%design%';
GO
