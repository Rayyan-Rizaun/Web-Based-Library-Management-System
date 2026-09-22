/* =============================================================================
   Library Management System — Seed data, Part 2: Catalog and Inventory
   IT2140 Part 02 · Group MLB-B11G2-10 · Microsoft SQL Server 2022
   -----------------------------------------------------------------------------
   Run after database/02_seed_01_identity.sql on the same database.
   Seeds, in FK-safe order: Publisher, Author, Category, Book, BookAuthor,
   BookCategory, BookKeyword, BookCopy.

   IDs are deterministic (Publisher 1-6, Author 1-15, Category 1-8, Book
   1-12, BookCopy 1-42), because these tables are empty before this script
   runs — later seed files rely on these exact numbers.

   Coverage required by this file:
     - BookID 1 (Clean Code): all 4 copies Status = 'On Loan' — the title
       the reservation queue is built on in chunk 3.
     - BookID 3 (Software Engineering): one copy Lost, one copy Damaged.
     - Two reference-only copies (BookID 2 copy 4, BookID 11 copy 4):
       IsReferenceOnly = 1, Status = 'Available' (cannot be On Loan/On Hold).
     - CopyCondition uses all four values; Status uses all seven values
       (Available, On Loan, On Hold, Under Repair, Damaged, Lost, Withdrawn).
     - Author overlap (R.K. Bansal on two Civil Engineering titles) and
       category overlap (Computer Science / Software Engineering across
       three titles; Law and English Literature each across two titles) so
       catalogue filters actually narrow the result set.
   ============================================================================= */

SET NOCOUNT ON;
GO

/* -----------------------------------------------------------------------------
   1. Publisher  (6 rows)
   ----------------------------------------------------------------------------- */
INSERT INTO dbo.Publisher (PublisherName, Website, IsActive) VALUES
    (N'Pearson Education',            N'https://www.pearson.com',              1),  -- 1
    (N'McGraw-Hill Education',        N'https://www.mheducation.com',          1),  -- 2
    (N'Cengage Learning',             N'https://www.cengage.com',              1),  -- 3
    (N'Cambridge University Press',   N'https://www.cambridge.org',            1),  -- 4
    (N'Sarasavi Publishers',          N'https://www.sarasavi.lk',              1),  -- 5
    (N'Vijitha Yapa Publications',    N'https://www.vijithayapa.com',          1);  -- 6
GO

/* -----------------------------------------------------------------------------
   2. Author  (15 rows)
   ----------------------------------------------------------------------------- */
INSERT INTO dbo.Author (AuthorName, Biography, IsActive) VALUES
    (N'Robert C. Martin',      N'Software engineer and author, known for the Agile Manifesto and the SOLID principles.', 1),  -- 1
    (N'Thomas H. Cormen',      N'Professor of Computer Science at Dartmouth College; co-author of the standard algorithms textbook.', 1),  -- 2
    (N'Ian Sommerville',       N'Professor Emeritus of Software Engineering at St Andrews, author of the widely used Software Engineering textbook.', 1),  -- 3
    (N'Erich Gamma',           N'Software engineer, one of the "Gang of Four" design patterns authors.', 1),  -- 4
    (N'Richard Helm',          N'Software engineer and co-author of the Design Patterns book.', 1),  -- 5
    (N'Philip Kotler',         N'Professor of Marketing at Northwestern University, widely regarded as the father of modern marketing.', 1),  -- 6
    (N'Gary Armstrong',        N'Professor of Marketing at the University of North Carolina, co-author of Principles of Marketing.', 1),  -- 7
    (N'Stephen P. Robbins',    N'Professor Emeritus of Management at San Diego State University.', 1),  -- 8
    (N'Mary Coulter',          N'Professor of Management, co-author of the standard Management textbook.', 1),  -- 9
    (N'R.K. Bansal',           N'Author of widely used engineering mechanics and structural analysis textbooks.', 1),  -- 10
    (N'S.C. Rangwala',         N'Civil engineering author, co-author of Theory of Structures.', 1),  -- 11
    (N'H.W. Tambiah',          N'Sri Lankan legal scholar and author on the Ceylon legal system.', 1),  -- 12
    (N'Sir Ivor Jennings',     N'Constitutional lawyer and Vice-Chancellor of the University of Ceylon.', 1),  -- 13
    (N'George Orwell',         N'English novelist and essayist, author of Animal Farm and Nineteen Eighty-Four.', 1),  -- 14
    (N'Martin Wickramasinghe', N'Pioneering Sri Lankan novelist, author of the Sinhala classic Gamperaliya.', 1);  -- 15
GO

/* -----------------------------------------------------------------------------
   3. Category  (8 rows)
   ----------------------------------------------------------------------------- */
INSERT INTO dbo.Category (CategoryName, Description, IsActive) VALUES
    (N'Computer Science',      N'Theory and foundations of computing.', 1),           -- 1
    (N'Software Engineering',  N'Software design, architecture and process.', 1),     -- 2
    (N'Management & Business', N'General management, organisational behaviour and business practice.', 1),  -- 3
    (N'Marketing',             N'Marketing theory and practice.', 1),                 -- 4
    (N'Civil Engineering',     N'Structural analysis, mechanics of materials and construction.', 1),  -- 5
    (N'Law',                   N'Legal systems, constitutional law and jurisprudence.', 1),  -- 6
    (N'Sinhala Literature',    N'Sinhala-language fiction and literary criticism.', 1),  -- 7
    (N'English Literature',    N'English-language fiction, poetry and literary criticism.', 1);  -- 8
GO

/* -----------------------------------------------------------------------------
   4. Book  (12 rows)
   ----------------------------------------------------------------------------- */
INSERT INTO dbo.Book (ISBN13, ISBN10, Title, Subtitle, PublisherID, PublicationYear, Edition, LanguageCode, Description) VALUES
    (N'9780132350884', NULL, N'Clean Code',                              N'A Handbook of Agile Software Craftsmanship', 1, 2016, N'1st',  N'en', N'A guide to writing readable, maintainable software.'),                          -- 1
    (N'9780262033848', NULL, N'Introduction to Algorithms',              NULL,                                          2, 2017, N'3rd',  N'en', N'A comprehensive textbook on algorithms and data structures.'),                  -- 2
    (N'9780133943030', NULL, N'Software Engineering',                    NULL,                                          1, 2015, N'10th', N'en', N'A standard textbook on software engineering principles and practice.'),         -- 3
    (N'9780451526342', NULL, N'Animal Farm',                             NULL,                                          6, 2020, N'Reprint', N'en', N'A satirical allegorical novella set on an English farm.'),                    -- 4
    (N'9780201633610', NULL, N'Design Patterns',                         N'Elements of Reusable Object-Oriented Software', 1, 2019, N'1st', N'en', N'The classic catalogue of object-oriented design patterns.'),                 -- 5
    (N'9781292220178', NULL, N'Principles of Marketing',                 NULL,                                          1, 2021, N'17th', N'en', N'A foundational textbook on marketing theory and practice.'),                    -- 6
    (N'9781292090003', NULL, N'Management',                              NULL,                                          1, 2022, N'13th', N'en', N'A widely used introductory management textbook.'),                              -- 7
    (N'9788131808146', NULL, N'Strength of Materials',                   NULL,                                          3, 2018, N'6th',  N'en', N'A core textbook on mechanics of solids for civil engineering students.'),       -- 8
    (N'9788131808726', NULL, N'Theory of Structures',                    NULL,                                          3, 2019, N'4th',  N'en', N'A textbook on structural analysis of determinate and indeterminate structures.'), -- 9
    (N'9789550001234', NULL, N'Ceylon Law Reports',                      N'An Introduction to the Legal System',        6, 2017, NULL,    N'en', N'An introductory survey of the Sri Lankan legal system and case law.'),          -- 10
    (N'9781107612249', NULL, N'Constitutional Law of Ceylon',            NULL,                                          4, 2020, NULL,    N'en', N'A study of the constitutional and legal history of Ceylon.'),                   -- 11
    (N'9789550009876', NULL, N'Gamperaliya',                             NULL,                                          5, 2023, NULL,    N'si', N'A landmark Sinhala novel depicting the decline of a feudal village family.');   -- 12
GO

/* -----------------------------------------------------------------------------
   5. BookAuthor  (M:N WRITES, 14 rows)
   ----------------------------------------------------------------------------- */
INSERT INTO dbo.BookAuthor (BookID, AuthorID, AuthorOrder) VALUES
    (1,  1,  1),                 -- Clean Code: Robert C. Martin
    (2,  2,  1),                 -- Introduction to Algorithms: Thomas H. Cormen
    (3,  3,  1),                 -- Software Engineering: Ian Sommerville
    (4,  14, 1),                 -- Animal Farm: George Orwell
    (5,  4,  1), (5, 5, 2),      -- Design Patterns: Gamma, Helm
    (6,  6,  1), (6, 7, 2),      -- Principles of Marketing: Kotler, Armstrong
    (7,  8,  1), (7, 9, 2),      -- Management: Robbins, Coulter
    (8,  10, 1),                 -- Strength of Materials: R.K. Bansal
    (9,  10, 1), (9, 11, 2),     -- Theory of Structures: Bansal, Rangwala
    (10, 12, 1),                 -- Ceylon Law Reports: H.W. Tambiah
    (11, 13, 1),                 -- Constitutional Law of Ceylon: Sir Ivor Jennings
    (12, 15, 1);                 -- Gamperaliya: Martin Wickramasinghe
GO

/* -----------------------------------------------------------------------------
   6. BookCategory  (M:N CLASSIFIES, 16 rows)
   ----------------------------------------------------------------------------- */
INSERT INTO dbo.BookCategory (BookID, CategoryID) VALUES
    (1, 2),  (1, 1),    -- Clean Code: Software Engineering, Computer Science
    (2, 1),             -- Introduction to Algorithms: Computer Science
    (3, 2),  (3, 1),    -- Software Engineering: Software Engineering, Computer Science
    (4, 8),             -- Animal Farm: English Literature
    (5, 2),  (5, 1),    -- Design Patterns: Software Engineering, Computer Science
    (6, 4),  (6, 3),    -- Principles of Marketing: Marketing, Management & Business
    (7, 3),             -- Management: Management & Business
    (8, 5),             -- Strength of Materials: Civil Engineering
    (9, 5),             -- Theory of Structures: Civil Engineering
    (10, 6),            -- Ceylon Law Reports: Law
    (11, 6),            -- Constitutional Law of Ceylon: Law
    (12, 7), (12, 8);   -- Gamperaliya: Sinhala Literature, English Literature
GO

/* -----------------------------------------------------------------------------
   7. BookKeyword  (multivalued Keywords attribute, 48 rows)
   ----------------------------------------------------------------------------- */
INSERT INTO dbo.BookKeyword (BookID, Keyword) VALUES
    (1,  N'clean code'), (1,  N'refactoring'), (1,  N'software craftsmanship'), (1,  N'best practices'),
    (2,  N'algorithms'), (2,  N'data structures'), (2,  N'complexity analysis'), (2,  N'sorting'),
    (3,  N'software process'), (3,  N'requirements engineering'), (3,  N'software design'), (3,  N'testing'), (3, N'agile'),
    (4,  N'satire'), (4,  N'allegory'), (4,  N'politics'), (4,  N'dystopia'),
    (5,  N'design patterns'), (5,  N'object-oriented design'), (5,  N'gang of four'), (5,  N'reusability'),
    (6,  N'marketing mix'), (6,  N'consumer behaviour'), (6,  N'branding'), (6,  N'market segmentation'),
    (7,  N'planning'), (7,  N'organizing'), (7,  N'leadership'), (7,  N'decision making'), (7, N'control'),
    (8,  N'stress and strain'), (8,  N'beams'), (8,  N'torsion'), (8,  N'mechanics of solids'),
    (9,  N'structural analysis'), (9,  N'trusses'), (9,  N'columns'), (9,  N'indeterminate structures'),
    (10, N'sri lankan law'), (10, N'case law'), (10, N'legal system'), (10, N'jurisprudence'),
    (11, N'constitutional law'), (11, N'ceylon'), (11, N'governance'), (11, N'legal history'),
    (12, N'sinhala novel'), (12, N'village life'), (12, N'social change'), (12, N'wickramasinghe');
GO

/* -----------------------------------------------------------------------------
   8. BookCopy  (42 rows: 3-4 per title)
      BookID 1 (Clean Code) is the fully-checked-out title: all 4 copies are
      Status = 'On Loan'. A later seed file inserts the matching Loan rows
      and builds a reservation queue on it.
      BookID 3 (Software Engineering) carries one Lost and one Damaged copy.
      Two copies (BookID 2 copy 4, BookID 11 copy 4) are reference-only.
   ----------------------------------------------------------------------------- */
INSERT INTO dbo.BookCopy (BookID, AccessionNumber, Barcode, ShelfLocation, AcquisitionDate, PurchasePrice, CopyCondition, Status, IsReferenceOnly) VALUES
    -- Clean Code (4 copies) -- fully checked out
    (1,  N'ACC-1001', N'BC-20001', N'CS-A-01',  '2016-08-10', 4800.00, N'Good', N'On Loan', 0),
    (1,  N'ACC-1002', N'BC-20002', N'CS-A-01',  '2016-08-10', 4800.00, N'Good', N'On Loan', 0),
    (1,  N'ACC-1003', N'BC-20003', N'CS-A-01',  '2019-03-15', 5200.00, N'New',  N'On Loan', 0),
    (1,  N'ACC-1004', N'BC-20004', N'CS-A-01',  '2019-03-15', 5200.00, N'Fair', N'On Loan', 0),

    -- Introduction to Algorithms (4 copies) -- one reference-only
    (2,  N'ACC-1005', N'BC-20005', N'CS-A-02',  '2017-09-01', 7800.00, N'Good', N'Available', 0),
    (2,  N'ACC-1006', N'BC-20006', N'CS-A-02',  '2017-09-01', 7800.00, N'Good', N'Available', 0),
    (2,  N'ACC-1007', N'BC-20007', N'CS-A-02',  '2020-01-20', 8200.00, N'New',  N'Available', 0),
    (2,  N'ACC-1008', N'BC-20008', N'CS-A-02',  '2020-01-20', 8200.00, N'New',  N'Available', 1),  -- reference-only

    -- Software Engineering (4 copies) -- one Lost, one Damaged
    (3,  N'ACC-1009', N'BC-20009', N'CS-A-03',  '2015-07-05', 6500.00, N'Fair', N'Available', 0),
    (3,  N'ACC-1010', N'BC-20010', N'CS-A-03',  '2015-07-05', 6500.00, N'Good', N'Available', 0),
    (3,  N'ACC-1011', N'BC-20011', N'CS-A-03',  '2018-02-14', 6900.00, N'Poor', N'Lost', 0),
    (3,  N'ACC-1012', N'BC-20012', N'CS-A-03',  '2018-02-14', 6900.00, N'Poor', N'Damaged', 0),

    -- Animal Farm (3 copies)
    (4,  N'ACC-1013', N'BC-20013', N'LIT-E-01', '2020-11-01', 1200.00, N'Good', N'Available', 0),
    (4,  N'ACC-1014', N'BC-20014', N'LIT-E-01', '2020-11-01', 1200.00, N'Fair', N'Available', 0),
    (4,  N'ACC-1015', N'BC-20015', N'LIT-E-01', '2021-05-20', 1350.00, N'Poor', N'Withdrawn', 0),

    -- Design Patterns (3 copies)
    (5,  N'ACC-1016', N'BC-20016', N'CS-A-04',  '2019-06-10', 5300.00, N'Good', N'Available', 0),
    (5,  N'ACC-1017', N'BC-20017', N'CS-A-04',  '2019-06-10', 5300.00, N'New',  N'Available', 0),
    (5,  N'ACC-1018', N'BC-20018', N'CS-A-04',  '2022-08-01', 5800.00, N'Good', N'Available', 0),

    -- Principles of Marketing (4 copies)
    (6,  N'ACC-1019', N'BC-20019', N'MGT-B-01', '2021-10-05', 4200.00, N'Good', N'Available', 0),
    (6,  N'ACC-1020', N'BC-20020', N'MGT-B-01', '2021-10-05', 4200.00, N'Fair', N'Available', 0),
    (6,  N'ACC-1021', N'BC-20021', N'MGT-B-01', '2023-02-18', 4500.00, N'New',  N'On Loan', 0),
    (6,  N'ACC-1022', N'BC-20022', N'MGT-B-01', '2023-02-18', 4500.00, N'Good', N'On Loan', 0),

    -- Management (3 copies)
    (7,  N'ACC-1023', N'BC-20023', N'MGT-B-02', '2022-09-12', 4700.00, N'Good', N'Available', 0),
    (7,  N'ACC-1024', N'BC-20024', N'MGT-B-02', '2022-09-12', 4700.00, N'New',  N'Available', 0),
    (7,  N'ACC-1025', N'BC-20025', N'MGT-B-02', '2023-11-30', 4900.00, N'Good', N'On Loan', 0),

    -- Strength of Materials (4 copies)
    (8,  N'ACC-1026', N'BC-20026', N'ENG-C-01', '2018-04-22', 3200.00, N'Good', N'Available', 0),
    (8,  N'ACC-1027', N'BC-20027', N'ENG-C-01', '2018-04-22', 3200.00, N'Fair', N'Available', 0),
    (8,  N'ACC-1028', N'BC-20028', N'ENG-C-01', '2021-07-15', 3500.00, N'New',  N'Available', 0),
    (8,  N'ACC-1029', N'BC-20029', N'ENG-C-01', '2021-07-15', 3500.00, N'Poor', N'Available', 0),

    -- Theory of Structures (3 copies)
    (9,  N'ACC-1030', N'BC-20030', N'ENG-C-02', '2019-09-09', 3300.00, N'Good', N'Available', 0),
    (9,  N'ACC-1031', N'BC-20031', N'ENG-C-02', '2019-09-09', 3300.00, N'Fair', N'Available', 0),
    (9,  N'ACC-1032', N'BC-20032', N'ENG-C-02', '2022-03-25', 3600.00, N'Poor', N'Under Repair', 0),

    -- Ceylon Law Reports (3 copies)
    (10, N'ACC-1033', N'BC-20033', N'LAW-D-01', '2017-11-11', 2800.00, N'Good', N'Available', 0),
    (10, N'ACC-1034', N'BC-20034', N'LAW-D-01', '2017-11-11', 2800.00, N'Fair', N'Available', 0),
    (10, N'ACC-1035', N'BC-20035', N'LAW-D-01', '2020-06-06', 3000.00, N'Good', N'On Hold', 0),

    -- Constitutional Law of Ceylon (4 copies) -- one reference-only
    (11, N'ACC-1036', N'BC-20036', N'LAW-D-02', '2020-08-19', 5100.00, N'Good', N'Available', 0),
    (11, N'ACC-1037', N'BC-20037', N'LAW-D-02', '2020-08-19', 5100.00, N'New',  N'Available', 0),
    (11, N'ACC-1038', N'BC-20038', N'LAW-D-02', '2023-01-10', 5400.00, N'Good', N'Available', 0),
    (11, N'ACC-1039', N'BC-20039', N'LAW-D-02', '2023-01-10', 5400.00, N'New',  N'Available', 1),  -- reference-only

    -- Gamperaliya (3 copies)
    (12, N'ACC-1040', N'BC-20040', N'LIT-E-02', '2023-06-01', 950.00,  N'Good', N'Available', 0),
    (12, N'ACC-1041', N'BC-20041', N'LIT-E-02', '2023-06-01', 950.00,  N'New',  N'Available', 0),
    (12, N'ACC-1042', N'BC-20042', N'LIT-E-02', '2024-02-14', 1050.00, N'Fair', N'Available', 0);
GO
