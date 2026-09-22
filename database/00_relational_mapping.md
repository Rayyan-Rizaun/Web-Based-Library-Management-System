# IT2140 Part 02 — Part A: EER → Relational Schema Mapping
Group MLB-B11G2-10 · Target: Microsoft SQL Server 2022 · No DDL in this document

Sources: `docs/er-diagram.png` (EER), `docs/requirements.pdf`, `docs/scenarios.pdf`
(UC-01 … UC-10), `docs/business-rules.md` (binding where sources disagree),
`docs/it2140-marking-schema.pdf`.

---

## 0. Conventions used below

- **PK** = primary key, **AK** = alternate key (UNIQUE NOT NULL), **FK** = foreign key.
  Nullable FKs are marked *(NULL)*; every other FK is NOT NULL.
- Surrogate `INT IDENTITY` keys are kept exactly as the diagram names them, so
  every FK in the diagram still resolves.
- **Delete behaviour.** FKs are `NO ACTION` unless a row says `CASCADE`. Users,
  books and copies are deactivated (`Status` / `IsActive`), never deleted.
  SQL Server also rejects a table with more than one cascade path to the same
  parent, and about fifteen tables reference APP_USER.
- **Where each rule is enforced:**
  - *[CHECK]* — a single-row rule, written as a CHECK constraint.
  - *[UQ-filtered]* — a filtered unique index. Use it when uniqueness only
    applies to some rows, such as "one open loan per copy".
  - *[TRG]* / *[PROC]* — a rule that has to read other rows or tables, so it
    belongs in a trigger or stored procedure (Parts E/F).
- Configurable numbers (14 days, LKR 20/day, 5/10 loans, 2 renewals, 3-day hold,
  LKR 500 cap, LKR 500 lost-book fee) live in SYSTEM_SETTING and are never
  hard-coded (business-rules §7).
- Relations are written in the EER's UPPER_SNAKE names here. The DDL
  (`01_schema.sql`) uses PascalCase names per CLAUDE.md rule 8, e.g.
  APP_USER → AppUser, BOOK_COPY → BookCopy. Its header maps each table back
  to its EER source.

### Source conflicts resolved before mapping
| Conflict | Resolution used here |
|---|---|
| Requirements: only Academic Staff may reserve. Business rules §3: any active member may reserve. | Business rules win. The `CanReserveUnavailableBooks` attribute on ACADEMIC_STAFF is dropped (see §4). |
| UC-09 publishes reviews immediately. Business rules §6: reviews are moderated. | Moderated. BOOK_REVIEW.Status starts at `Pending`. |
| UC-10 feedback statuses are Pending/Reviewed/Resolved. Business rules §6: Submitted → Under Review → In Progress → Resolved → Closed. | Business rules win. |
| RESERVATION.Priority attribute vs business rules §3 FIFO queue. | FIFO. Priority is dropped (see §4). |
| Business rules §5 said "BOOK.price". The diagram has no price on BOOK, only BOOK_COPY.PurchasePrice. | **Resolved.** business-rules.md §5 now names `BOOK_COPY.PurchasePrice`, a per-copy replacement cost, matching the diagram. Lost = PurchasePrice + processing fee; the damaged charge is capped at PurchasePrice. |

---

## 1. ISA mapping

Three textbook strategies exist:
**(A) ER / table-per-entity.** One table for the supertype and one per subtype, joined on a 1:1 key.
**(B) OO / table-per-concrete-class.** Subtype tables repeat every inherited attribute, and there is no supertype table.
**(C) Single table.** One table holds all attributes plus a type discriminator, and subtype columns are nullable.

### 1.1 APP_USER → {STAFF_PROFILE, MEMBER} — overlapping, partial → **Strategy A (ER)**

> **Declared change:** Part 01 specified this ISA as *disjoint*, partial. It is
> mapped here as *overlapping*, partial. The reasons and evidence are in
> refinement **#30** (§4).

**Why A beats B and C.**
- **Against B (OO).** A librarian who also borrows books is legal here, because the ISA is overlapping. Under B that person would hold two copies of Email, PasswordHash and Name. A password change would have to update both, and login would need a UNION across tables.
- **Against C (single table).** Every MEMBER-only FK (LOAN, RESERVATION, FINE, BOOK_REVIEW, …) and every staff-only FK (who issued a loan, who moderated a review) would point at the same wide table. Any UserID would then pass the FK, so SQL Server could not stop a staff-only account from borrowing, or a member from being recorded as the issuer. C would also need two type flags instead of one, because the ISA overlaps. EmployeeNo and MembershipNo would be NULL on most rows and need filtered unique indexes.
- **What A gives the real query paths.** Login reads only the narrow APP_USER table. Borrowing and fine checks join LOAN straight to MEMBER. Staff actions get FKs that target STAFF_PROFILE. Partial participation is free: an APP_USER row with no subtype row is simply a user with neither profile.

**How A is realised.** The diagram gives each subtype its own surrogate PK (StaffID, MemberID) plus `AK/FK UserID`.
- This is kept rather than collapsed into a shared PK, because every downstream table already references MemberID or StaffID.
- `UserID UNIQUE NOT NULL FK → APP_USER` enforces the 1:1 subtype link exactly as a shared PK would.
- Overlap needs no extra constraint: nothing stops one UserID from appearing in both subtype tables.

### 1.2 MEMBER → ACADEMIC_STAFF — disjoint, partial → **Strategy C (single table)**

**Why C beats A and B.**
- **Nothing left for a subtype table.** After refinement (§4), ACADEMIC_STAFF has no stored attributes of its own:
  - `BorrowingLimit` is a SYSTEM_SETTING value keyed by member type.
  - `CanReserveUnavailableBooks` is obsolete under business-rules §3.
- **Against A (ER).** A would create a table holding nothing but a key. The borrow-eligibility check runs on every issue, renewal and reservation, and it would need an extra EXISTS/LEFT JOIN just to learn the member's type. Disjointness would also need a trigger to stop a MemberID appearing as both types.
- **Against B (OO).** B would split members into STUDENT_MEMBER and ACADEMIC_STAFF tables. The eight tables that reference "a member" would then each need two nullable FKs plus an XOR check:
  - LOAN, RESERVATION, FINE, BOOK_INCIDENT
  - BOOK_REVIEW, REVIEW_FLAG, MEMBER_FEEDBACK, FINE_APPEAL

  Promoting a student to staff would also mean delete + re-insert under a new key, breaking their loan and fine history.
- **What C gives.** One NOT NULL discriminator enforces disjointness for free, since a row can hold only one value. This matches the diagram's own note, `Implemented by MemberType = 'Academic Staff'`.
- **Partial participation.** A member who is not academic staff has `MemberType = 'Student'`, the only other member type in business rules §4.

---

## 2. Aggregation over MEMBER — BORROWS — BOOK_COPY (the LOAN transaction)

**What the diagram shows.** BORROWS is an M:N between MEMBER and BOOK_COPY. It happens *over time*: the same member can borrow the same copy many times. The diagram draws an aggregation box around it so the borrowing *event* can take part in higher-level relationships:
- RENEWALS → LOAN_RENEWAL
- OVERDUE SOURCE → FINE
- ARISES FROM → BOOK_INCIDENT
- ISSUED/RETURNED BY → STAFF_PROFILE

**Mapping.**
1. The aggregated relationship becomes one relation, **LOAN**. It carries FKs to both participants (`MemberID → MEMBER`, `CopyID → BOOK_COPY`) plus the relationship's attributes (BorrowedAt, DueAt, ReturnedAt, …).
2. `(MemberID, CopyID)` cannot be the key, because repeat borrowing is allowed. The natural candidate key is `(CopyID, BorrowedAt)`, since one copy cannot be issued twice at the same instant. It is kept as an AK.
3. The aggregate is given the surrogate PK `LoanID`. Every higher-level relationship references the aggregate by `LoanID`:
   - `LOAN_RENEWAL.LoanID`
   - `FINE.LoanID`
   - `BOOK_INCIDENT.LoanID`

   Without it, those tables would each carry the three-column composite (CopyID, BorrowedAt, MemberID). This is the relational meaning of "a relationship participating in a relationship".
4. The aggregation also carries an integrity rule the plain M:N could not express: **a copy has at most one open loan**. This becomes *[UQ-filtered]* `UNIQUE (CopyID) WHERE ReturnedAt IS NULL`.
5. LOAN stores no BookID. The title comes from `CopyID → BOOK_COPY.BookID`, so storing it again would be a transitive dependency.

---

## 3. Relational schema

### 3.1 Identity and access

**ROLE** (RoleID, RoleName, RoleDescription, IsActive)
- PK RoleID · AK RoleName
- IsActive BIT NOT NULL DEFAULT 1

**USER_ROLE** (UserID, RoleID, AssignedAt, AssignedByUserID) — *from the M:N ASSIGNS ROLE*
- PK (UserID, RoleID)
- FK UserID → APP_USER(UserID) CASCADE · FK RoleID → ROLE(RoleID) · FK AssignedByUserID → APP_USER(UserID) *(NULL)*
- [CHECK] AssignedByUserID IS NULL OR AssignedByUserID <> UserID (nobody assigns their own role, UC-01)
- **Bootstrap:** NULL AssignedByUserID means "granted by the system". It is used only for the first Library Administrator, who has nobody to grant the role. A seeded "SYSTEM" user was rejected: it would need a fake Email, PasswordHash and Phone, would inflate user counts and reports, and would be a login-capable row. [TRG] reject NULL once any Library Administrator exists.
- [PROC] Only a Library Administrator may assign roles (UC-01)
- [TRG] A user holding the Member role must have a MEMBER row; a user holding a staff role must have a STAFF_PROFILE row

**APP_USER** (UserID, Email, PasswordHash, FirstName, LastName, Phone, AddressLine1, AddressLine2, City, Status, CreatedAt, UpdatedAt) — *supertype*
- PK UserID · AK Email
- FirstName, LastName, PasswordHash, Phone NOT NULL (the security interview names contact number as a mandatory field)
- [CHECK] Address components together: all three NULL, or AddressLine1 and City both NOT NULL (AddressLine2 optional)
- [CHECK] Status IN ('Active','Deactivated','Locked') DEFAULT 'Active'
- [CHECK] Email LIKE '%_@_%._%' AND contains no spaces
- [CHECK] UpdatedAt >= CreatedAt

**STAFF_PROFILE** (StaffID, UserID, EmployeeNo, JobTitle, JoinedDate, IsActive) — *subtype, Strategy A*
- PK StaffID · AK UserID · AK EmployeeNo
- FK UserID → APP_USER(UserID)
- IsActive (employment) is separate from APP_USER.Status (login). A former librarian who is still a member stays able to log in.

**MEMBER** (MemberID, UserID, MembershipNo, NationalID, MemberType, JoinedDate, ExpiryDate, MembershipStatus) — *subtype, Strategy A; holds ACADEMIC_STAFF via Strategy C*
- PK MemberID · AK UserID · AK MembershipNo
- NationalID NULL · [UQ-filtered] UNIQUE (NationalID) WHERE NationalID IS NOT NULL. A member need not have one, but no two members share one (same pattern as BOOK.ISBN10).
- FK UserID → APP_USER(UserID)
- [CHECK] MemberType IN ('Student','Academic Staff') — ISA discriminator; disjoint by construction
- [CHECK] ExpiryDate > JoinedDate
- [CHECK] MembershipStatus IN ('Active','Suspended','Cancelled'). "Expired" is derived from ExpiryDate < today.
- [PROC] Borrowing limit = SYSTEM_SETTING row for MemberType (5 / 10)

### 3.2 Catalog and inventory

**PUBLISHER** (PublisherID, PublisherName, Website, IsActive)
- PK PublisherID · AK PublisherName

**AUTHOR** (AuthorID, AuthorName, Biography, IsActive)
- PK AuthorID

**CATEGORY** (CategoryID, CategoryName, Description, IsActive)
- PK CategoryID · AK CategoryName

**BOOK** (BookID, ISBN13, ISBN10, Title, Subtitle, PublisherID, PublicationYear, Edition, LanguageCode, Description, IsActive)
- PK BookID · AK ISBN13 (UC-02 duplicate-ISBN error)
- FK PublisherID → PUBLISHER(PublisherID) *(NULL)* — the 1:N PUBLISHED BY, which had no FK in the entity box
- [UQ-filtered] UNIQUE (ISBN10) WHERE ISBN10 IS NOT NULL
- [CHECK] LEN(ISBN13) = 13 AND ISBN13 NOT LIKE '%[^0-9]%'
- [CHECK] PublicationYear BETWEEN 1450 AND YEAR(GETDATE())+1
- [CHECK] LEN(LanguageCode) BETWEEN 2 AND 3
- Title NOT NULL, Subtitle NULL

**BOOK_AUTHOR** (BookID, AuthorID, AuthorOrder) — *from the M:N WRITES*
- PK (BookID, AuthorID)
- FK BookID → BOOK(BookID) CASCADE · FK AuthorID → AUTHOR(AuthorID)
- AK (BookID, AuthorOrder) — no two authors share a position
- [CHECK] AuthorOrder >= 1

**BOOK_CATEGORY** (BookID, CategoryID) — *from the M:N CLASSIFIES*
- PK (BookID, CategoryID)
- FK BookID → BOOK(BookID) CASCADE · FK CategoryID → CATEGORY(CategoryID)

**BOOK_KEYWORD** (BookID, Keyword) — *from the multivalued [M] Keywords*
- PK (BookID, Keyword)
- FK BookID → BOOK(BookID) CASCADE
- [CHECK] LEN(LTRIM(Keyword)) > 0

**BOOK_COPY** (CopyID, BookID, AccessionNumber, Barcode, ShelfLocation, AcquisitionDate, PurchasePrice, CopyCondition, Status, IsReferenceOnly)
- PK CopyID · AK AccessionNumber · AK Barcode
- FK BookID → BOOK(BookID)
- [CHECK] PurchasePrice >= 0
- [CHECK] CopyCondition IN ('New','Good','Fair','Poor')
- [CHECK] Status IN ('Available','On Loan','On Hold','Under Repair','Damaged','Lost','Withdrawn'). 'Under Repair' and 'Damaged' come from the Library Director's interview.
- [CHECK] IsReferenceOnly = 0 OR Status NOT IN ('On Loan','On Hold')
- [CHECK] AcquisitionDate ≤ today
- [TRG] Status is maintained by triggers on LOAN, RESERVATION and BOOK_INCIDENT (see §4, "Kept status columns")
- [TRG] A copy with IsReferenceOnly = 1 cannot be loaned

### 3.3 Circulation

**LOAN** (LoanID, MemberID, CopyID, IssuedByStaffID, ReturnedToStaffID, BorrowedAt, DueAt, ReturnedAt, ReturnCondition, Status) — *the aggregation, §2*
- PK LoanID · AK (CopyID, BorrowedAt)
- FK MemberID → MEMBER(MemberID)
- FK CopyID → BOOK_COPY(CopyID)
- FK IssuedByStaffID → STAFF_PROFILE(StaffID)
- FK ReturnedToStaffID → STAFF_PROFILE(StaffID) *(NULL)*
- [UQ-filtered] UNIQUE (CopyID) WHERE ReturnedAt IS NULL — one open loan per copy
- [CHECK] DueAt > BorrowedAt
- [CHECK] ReturnedAt IS NULL OR ReturnedAt >= BorrowedAt
- [CHECK] The return fields are recorded together or not at all:
  `(ReturnedAt IS NULL AND ReturnedToStaffID IS NULL AND ReturnCondition IS NULL)`
  `OR (ReturnedAt IS NOT NULL AND ReturnedToStaffID IS NOT NULL AND ReturnCondition IS NOT NULL)`
- [CHECK] Status IN ('Active','Returned','Lost'). "Overdue" is derived: ReturnedAt IS NULL AND DueAt < now.
- [CHECK] Status matches the return:
  `(Status = 'Returned' AND ReturnedAt IS NOT NULL) OR (Status IN ('Active','Lost') AND ReturnedAt IS NULL)`
- [CHECK] ReturnCondition IN ('Good','Fair','Poor','Damaged')
- [PROC] At issue time, all of these must hold (business rules §1, §4):
  - active loans < limit for the member's type
  - no overdue loans
  - no unpaid fines from an overdue return
  - membership not expired and MembershipStatus = 'Active'
  - DueAt = BorrowedAt + loan-period setting

**LOAN_RENEWAL** (RenewalID, LoanID, RequestedByUserID, RequestedAt, ApprovedByStaffID, OldDueAt, NewDueAt, Status)
- PK RenewalID
- FK LoanID → LOAN(LoanID)
- FK RequestedByUserID → APP_USER(UserID) — a member, or a librarian on the member's behalf
- FK ApprovedByStaffID → STAFF_PROFILE(StaffID) *(NULL)* — NULL when the system auto-approves
- [CHECK] NewDueAt > OldDueAt
- [CHECK] Status IN ('Pending','Approved','Rejected')
- [CHECK] ApprovedByStaffID IS NULL OR Status = 'Approved'
- [TRG] At most 2 approved renewals per loan
- [PROC] Refuse the renewal if the title has a Waiting or Ready reservation by another member
- [PROC] On approval, set LOAN.DueAt = NewDueAt

**RESERVATION** (ReservationID, BookID, MemberID, RequestedAt, Status, ReadyAt, ExpiresAt, ClosedAt) — *queue is per title (business rules §3)*
- PK ReservationID
- FK BookID → BOOK(BookID) · FK MemberID → MEMBER(MemberID)
- [UQ-filtered] UNIQUE (BookID, MemberID) WHERE Status IN ('Waiting','Ready') — UC-04 "already reserved" error
- [CHECK] Status IN ('Waiting','Ready','Fulfilled','Cancelled','Expired')
- [CHECK] Hold window as a pair: `(ReadyAt IS NULL AND ExpiresAt IS NULL) OR (ReadyAt IS NOT NULL AND ExpiresAt IS NOT NULL AND ExpiresAt > ReadyAt)`
- [CHECK] `(Status = 'Waiting' AND ReadyAt IS NULL) OR (Status IN ('Ready','Fulfilled','Expired') AND ReadyAt IS NOT NULL) OR Status = 'Cancelled'`
- [CHECK] `(Status IN ('Waiting','Ready') AND ClosedAt IS NULL) OR (Status IN ('Fulfilled','Cancelled','Expired') AND ClosedAt IS NOT NULL)`
- [CHECK] ReadyAt ≥ RequestedAt · ClosedAt ≥ RequestedAt
- [PROC] ExpiresAt = ReadyAt + hold-window setting (3 days)
- [PROC] Refuse the reservation if an available copy exists (UC-04: suggest borrowing instead)
- Queue position is derived: `ROW_NUMBER() OVER (PARTITION BY BookID ORDER BY RequestedAt)` over Waiting rows

### 3.4 Fines, appeals and payments

**BOOK_INCIDENT** (IncidentID, CopyID, LoanID, MemberID, IncidentType, Description, ReportedAt, RecordedByStaffID, Status)
- PK IncidentID
- FK CopyID → BOOK_COPY(CopyID) — AFFECTS, mandatory
- FK LoanID → LOAN(LoanID) *(NULL)* — ARISES FROM
- FK MemberID → MEMBER(MemberID) *(NULL)* — LIABLE MEMBER
- FK RecordedByStaffID → STAFF_PROFILE(StaffID)
- [CHECK] IncidentType IN ('Lost','Damaged')
- [CHECK] Status IN ('Open','Charged','Resolved','Written Off')
- Description NOT NULL — the librarian's justification note (business rules §5)
- [TRG] If LoanID IS NOT NULL then CopyID = LOAN.CopyID and MemberID = LOAN.MemberID

**FINE** (FineID, MemberID, LoanID, IncidentID, FineType, RatePerDay, AmountAssessed, AssessedAt, Status, WaiverReason, WaivedByStaffID)
- PK FineID
- FK MemberID → MEMBER(MemberID) — CHARGED TO
- FK LoanID → LOAN(LoanID) *(NULL)* — OVERDUE SOURCE
- FK IncidentID → BOOK_INCIDENT(IncidentID) *(NULL)* — LOSS/DAMAGE SOURCE
- FK WaivedByStaffID → STAFF_PROFILE(StaffID) *(NULL)*
- [CHECK] FineType IN ('Overdue','Lost','Damaged')
- [CHECK] FineType = 'Overdue' ⇒ LoanID NOT NULL AND IncidentID IS NULL AND RatePerDay NOT NULL
- [CHECK] FineType IN ('Lost','Damaged') ⇒ IncidentID NOT NULL AND LoanID IS NULL AND RatePerDay IS NULL. For an incident fine, the loan is reached through BOOK_INCIDENT.LoanID, so FINE does not store it a second time (same rule as #19).
- [CHECK] AmountAssessed > 0 · RatePerDay > 0
- [CHECK] Status IN ('Pending','Under Appeal','Partially Paid','Fully Paid','Waived')
- [CHECK] (Status = 'Waived') ⇔ (WaiverReason NOT NULL AND WaivedByStaffID NOT NULL)
- [UQ-filtered] UNIQUE (LoanID) WHERE FineType = 'Overdue' — one overdue fine per loan, so the per-loan cap has one row to apply to
- [UQ-filtered] UNIQUE (IncidentID) WHERE IncidentID IS NOT NULL
- [PROC] Charge formulas:
  - Overdue: AmountAssessed = MIN(days × RatePerDay, cap)
  - Lost: AmountAssessed = PurchasePrice + processing fee
  - Damaged: AmountAssessed ≤ PurchasePrice
- [TRG] MemberID must equal the loan's member (Overdue fines) or the incident's liable member (Lost/Damaged fines)

**FINE_APPEAL** (AppealID, FineID, AppealReason, SubmittedAt, Status, DecidedByStaffID, DecidedAt, DecisionComments, ApprovedReduction)
- PK AppealID
- FK FineID → FINE(FineID)
- FK DecidedByStaffID → STAFF_PROFILE(StaffID) *(NULL)*
- [CHECK] Status IN ('Pending','Approved','Rejected')
- [CHECK] Status = 'Pending' ⇔ (DecidedByStaffID IS NULL AND DecidedAt IS NULL)
- [CHECK] (Status = 'Approved') ⇔ ApprovedReduction NOT NULL
- [CHECK] ApprovedReduction > 0
- [UQ-filtered] UNIQUE (FineID) WHERE Status = 'Pending' — one open appeal at a time
- [TRG] ApprovedReduction ≤ outstanding balance; the appeal's member is the fine's member
- [TRG] A Pending appeal sets FINE.Status = 'Under Appeal' and pauses payment; the decision restores or updates the fine's status (business rules §2)

**FINE_PAYMENT** (PaymentID, FineID, ReceiptNumber, AmountPaid, PaymentMethod, PaymentStatus, PaidAt, ReceivedByStaffID)
- PK PaymentID · AK ReceiptNumber (UC-07 "unique receipt")
- FK FineID → FINE(FineID)
- FK ReceivedByStaffID → STAFF_PROFILE(StaffID) *(NULL)* — NULL for online self-payment; NOT NULL for counter cash (UC-07 alternative flow)
- [CHECK] AmountPaid > 0
- [CHECK] PaymentMethod IN ('Cash','Card','Online')
- [CHECK] PaymentStatus IN ('Completed','Failed','Refunded')
- [CHECK] PaymentMethod = 'Cash' ⇒ ReceivedByStaffID NOT NULL
- [TRG] Reject payments while the fine is 'Under Appeal' or 'Waived'
- [TRG] Completed payments may not exceed the outstanding balance
- [TRG] After each payment, set FINE.Status to 'Partially Paid' or 'Fully Paid'

> **Outstanding balance** (never stored) = `AmountAssessed − ISNULL(SUM(approved ApprovedReduction),0) − ISNULL(SUM(Completed AmountPaid),0)`, and 0 when Status = 'Waived'.

### 3.5 Reviews and feedback

**BOOK_REVIEW** (ReviewID, BookID, MemberID, Rating, ReviewText, Status, SubmittedAt, UpdatedAt)
- PK ReviewID · AK (BookID, MemberID) — one review per member per book; UC-09 edits instead of duplicating
- FK BookID → BOOK(BookID) · FK MemberID → MEMBER(MemberID)
- [CHECK] Rating BETWEEN 1 AND 5
- [CHECK] Status IN ('Pending','Approved','Rejected','Hidden','Removed') DEFAULT 'Pending'
- ReviewText NULL — the written review is optional
- [TRG] Insert only if the member has a LOAN of any copy of this book (UC-09 precondition)

**REVIEW_FLAG** (ReviewFlagID, ReviewID, ReportedByMemberID, Reason, Status, ReportedAt)
- PK ReviewFlagID · AK (ReviewID, ReportedByMemberID)
- FK ReviewID → BOOK_REVIEW(ReviewID) · FK ReportedByMemberID → MEMBER(MemberID)
- [CHECK] Status IN ('Open','Upheld','Dismissed')
- [TRG] Reporter ≠ review author ("another member", business rules §6)
- [TRG] A new flag moves an Approved review to 'Hidden'

**REVIEW_MODERATION_HISTORY** (ModerationID, ReviewID, PreviousStatus, NewStatus, Reason, ModeratorStaffID, ModeratedAt)
- PK ModerationID
- FK ReviewID → BOOK_REVIEW(ReviewID) · FK ModeratorStaffID → STAFF_PROFILE(StaffID) *(NULL)*
- [CHECK] PreviousStatus <> NewStatus; both use BOOK_REVIEW's status domain
- [CHECK] NewStatus = 'Rejected' ⇒ Reason NOT NULL (business rules §6)
- [CHECK] ModeratorStaffID IS NOT NULL OR NewStatus = 'Hidden'. The only transition without a human moderator is the automatic hide when a review is flagged (business rules §6).
- ModeratedAt NOT NULL — added, because the diagram's history had no timestamp

**FEEDBACK_CATEGORY** (FeedbackCategoryID, CategoryName, IsActive)
- PK FeedbackCategoryID · AK CategoryName

**MEMBER_FEEDBACK** (FeedbackID, FeedbackReference, MemberID, FeedbackCategoryID, Subject, Description, Priority, Status, AdminResponse, SubmittedAt, UpdatedAt)
- PK FeedbackID · AK FeedbackReference
- FK MemberID → MEMBER(MemberID) · FK FeedbackCategoryID → FEEDBACK_CATEGORY(FeedbackCategoryID)
- [CHECK] Priority IN ('Low','Medium','High')
- [CHECK] Status IN ('Submitted','Under Review','In Progress','Resolved','Closed')
- [TRG] The member may edit only while Status = 'Submitted' and AdminResponse IS NULL (UC-08 alternative flow)

**FEEDBACK_HISTORY** (FeedbackHistoryID, FeedbackID, PreviousStatus, NewStatus, PreviousResponse, NewResponse, ChangedByUserID, ChangedAt)
- PK FeedbackHistoryID
- FK FeedbackID → MEMBER_FEEDBACK(FeedbackID) CASCADE (history is owned by its feedback; UC-10 withdrawal deletes the feedback) · FK ChangedByUserID → APP_USER(UserID) — added, so the history shows who made each change
- [CHECK] PreviousStatus <> NewStatus OR ISNULL(PreviousResponse,'') <> ISNULL(NewResponse,'')

### 3.6 Operations and governance

**NOTIFICATION** (NotificationID, UserID, NotificationType, Title, Message, IsRead, CreatedAt)
- PK NotificationID
- FK UserID → APP_USER(UserID) — RECEIVES
- [CHECK] NotificationType IN ('ReservationReady','DueSoon','Overdue','FineIssued','AppealDecision','RenewalDecision','ReviewDecision','FeedbackResponse','General')
- IsRead DEFAULT 0

**AUDIT_LOG** (AuditID, UserID, ActionName, EntityName, EntityID, Details, OccurredAt)
- PK AuditID
- FK UserID → APP_USER(UserID) *(NULL)* — PERFORMED BY; NULL for system actions
- [CHECK] Details IS NULL OR ISJSON(Details) = 1 — SQL Server 2022 has no JSON type, so JSON is stored as NVARCHAR(MAX)

**REPORT_AUDIT** (ReportAuditID, RequestedByUserID, ReportType, FilterJson, GeneratedAt)
- PK ReportAuditID
- FK RequestedByUserID → APP_USER(UserID) — GENERATED BY
- [CHECK] ReportType IN ('BookInventory','BorrowedBooks','ReturnedBooks','OverdueBooks','Reservations','MemberRegistration','MemberActivity','MostBorrowedBooks','LostDamagedBooks','FineCollection','OutstandingFines','FinePaymentHistory','WaivedFines','Revenue','Reviews','Feedback'). The list comes from UC-08 and the Administrator and Finance interviews.
- [CHECK] FilterJson IS NULL OR ISJSON(FilterJson) = 1

**SYSTEM_SETTING** (SettingKey, SettingValue, SettingDataType, Description, UpdatedByUserID, UpdatedAt)
- PK SettingKey (natural key, e.g. `Loan.PeriodDays`, `Borrowing.Limit.Academic Staff`)
- FK UpdatedByUserID → APP_USER(UserID) *(NULL)* — UPDATED BY
- [CHECK] SettingDataType IN ('Int','Decimal','String','Bool')
- [CHECK] SettingDataType = 'Int' ⇒ TRY_CAST(SettingValue AS INT) IS NOT NULL; same pattern for Decimal and Bool

**FAILED_LOGIN_ATTEMPT** (AttemptID, EmailTried, IPAddress, UserAgent, FailureReason, AttemptedAt)
- PK AttemptID
- **No FK, on purpose:** EmailTried may not belong to any account. An FK would block those rows from being logged.
- [CHECK] FailureReason IN ('UnknownEmail','BadPassword','AccountLocked','AccountDeactivated')

**DATABASE_BACKUP_LOG** (BackupLogID, BackupType, BackupPath, StartedAt, CompletedAt, Status, InitiatedByUserID, ErrorMessage)
- PK BackupLogID
- FK InitiatedByUserID → APP_USER(UserID) *(NULL)* — INITIATED BY; NULL for scheduled jobs
- [CHECK] BackupType IN ('Full','Differential','Log')
- [CHECK] Status IN ('Running','Succeeded','Failed')
- [CHECK] CompletedAt IS NULL OR CompletedAt >= StartedAt
- [CHECK] `(Status = 'Running' AND CompletedAt IS NULL) OR (Status IN ('Succeeded','Failed') AND CompletedAt IS NOT NULL)`
- [CHECK] Status = 'Failed' ⇒ ErrorMessage NOT NULL

**Relation count: 32.** There is no ACADEMIC_STAFF relation (Strategy C). Three M:N relationships (ASSIGNS ROLE, WRITES, CLASSIFIES) and the multivalued Keywords attribute add USER_ROLE, BOOK_AUTHOR, BOOK_CATEGORY and BOOK_KEYWORD. The time-based M:N relationships BORROWS and PLACES/RESERVES map to LOAN and RESERVATION.

---

## 4. Refinements since Part 01

Two decision rules drive the derived-value rows, so they can be defended consistently in the viva:
- **Derived values.** Drop any pure count, sum, rank or clock-based value. A trigger cannot fire because time passes, and a count is cheap to recompute. Keep a status column only when it mixes *decisions* (Waived, Lost, Withdrawn) with derivable states, and give it a single trigger as owner.
- **Propagated FKs.** Drop a copied FK when the path through the parent FK is always present (NOT NULL). Keep it, with a trigger checking agreement, when the parent FK is optional.

| # | Change | Reason | Anomaly / requirement addressed |
|---|---|---|---|
| 1 | Multivalued `[M] Keywords` on BOOK → new relation **BOOK_KEYWORD(BookID, Keyword)**, PK both columns | 1NF: a relation attribute must be atomic | A comma list could not be indexed or searched with `=`, could not stop duplicate keywords, and editing one keyword meant rewriting the string (update anomaly). Supports catalogue search (Req 6). |
| 2 | Composite `Name {FirstName, LastName}` → columns **FirstName, LastName** on APP_USER; no Name column | Composite attributes map to their simple components | A single Name string cannot be sorted or searched by surname, and every read would need string splitting. A stored FullName beside the parts would be an update anomaly. |
| 3 | Composite `Address {Line1, Line2, City}` → **AddressLine1, AddressLine2 (NULL), City** | Same rule; Line2 is optional | Reports can filter by City. No partial-update anomaly from editing a packed string. |
| 4 | M:N **ASSIGNS ROLE** (ROLE ↔ APP_USER, with AssignedAt and AssignedByUserID) → **USER_ROLE(UserID, RoleID, AssignedAt, AssignedByUserID)**, with a self-referencing FK for the grantor (nullable for the bootstrap grant only) | **(1) Roles outnumber subtypes.** UC-01 assigns one of four roles (Library Member, Librarian, Finance Officer, Library Administrator), matching Requirements §3. APP_USER has only two subtypes, and all three staff roles fall inside the single STAFF_PROFILE subtype, so subtype membership cannot say which staff role a person holds. Role has to be its own relationship. **(2) Fine duties cross roles.** UC-06 has the Administrator decide appeals "in consultation with the Finance Officer". The security interview gives the Librarian "Record fines and payments", which UC-05 gives the Finance Officer. A library that does not staff every post separately needs one STAFF_PROFILE person to hold several roles, and a single RoleID column would forbid that. *No source names one person holding two staff roles; the sources show the duties overlapping, and M:N is the mapping that does not forbid it.* **(3) Grant trail.** AssignedAt and AssignedByUserID record who granted each privilege and when. The Administrator's listed duties include "Manage users and permissions", and the security interview asks for actions to be logged "so we can track who performed each action". Revoking a grant deletes its row, so revocations go to AUDIT_LOG. **(4) Secondary: overlap.** Under the overlapping ISA (#30), a librarian who borrows holds both Librarian and Library Member. | A RoleID column on APP_USER would force a second account for a second role, colliding with the Email AK (#30). The relationship's attributes would have no home, and access grants would leave no trace of who authorised them. Supports UC-01 and the security NFR. *The diagram shows no cardinality on the ROLE side; M:N is inferred from the points above.* |
| 5 | M:N **WRITES** (AUTHOR ↔ BOOK, with AuthorOrder) → **BOOK_AUTHOR(BookID, AuthorID, AuthorOrder)**, AK (BookID, AuthorOrder) | M:N needs its own relation; AuthorOrder belongs to the pair | An AuthorID on BOOK allows one author only; repeating groups break 1NF. Search by author (Req 6). |
| 6 | M:N **CLASSIFIES** (CATEGORY ↔ BOOK) → **BOOK_CATEGORY(BookID, CategoryID)** | The diagram's BOOK has no CategoryID and no cardinality on the relationship | Cross-disciplinary books need several categories. Search and inventory report by category (UC-02, UC-08). |
| 7 | M:N **BORROWS** (MEMBER ↔ BOOK_COPY) → reified as **LOAN** with surrogate LoanID; AK (CopyID, BorrowedAt); filtered UNIQUE (CopyID) WHERE ReturnedAt IS NULL | Aggregation: the borrowing event takes part in RENEWALS, OVERDUE SOURCE and ARISES FROM (§2) | Repeat borrowing makes (MemberID, CopyID) non-unique. The filtered index prevents a copy being issued twice (UC-03 alternative flow). |
| 8 | M:N MEMBER ↔ BOOK over time (PLACES / RESERVES) → **RESERVATION**, filtered UNIQUE (BookID, MemberID) for Waiting/Ready | Queue is per title (business rules §3) | UC-04 "already reserved the same book" is enforced by the database, not only by Java. |
| 9 | 1:N **PUBLISHED BY** → add FK **BOOK.PublisherID** | Relationship drawn, but no FK in the BOOK box | Without it the relationship is lost in mapping. |
| 10 | Drop **MEMBER.BorrowingLimit**; read the limit from SYSTEM_SETTING by MemberType | Transitive dependency MemberID → MemberType → BorrowingLimit (3NF) | **Update anomaly:** changing policy 5→6 means updating every student row, and a missed row gives two students different limits. Promoting a member leaves a stale limit. Business rules §1 and §7. |
| 11 | Drop **ACADEMIC_STAFF.CanReserveUnavailableBooks**; ACADEMIC_STAFF is not a relation | Business rules §3: every active member may reserve. It was also MemberType → flag (3NF). | Removes a column that would contradict policy. Enables Strategy C (§1.2). |
| 12 | Drop **RESERVATION.QueuePosition**; derive it with ROW_NUMBER() over RequestedAt | Pure rank | **Update anomaly:** one cancellation would require renumbering every later row in that title's queue; a missed update gives two members the same position. |
| 13 | Drop **RESERVATION.Priority** | Business rules §3 fix FIFO; any priority policy would derive from MemberType | Avoids a transitive dependency and a column no rule reads. |
| 14 | Drop **LOAN.RenewalCount**; COUNT approved LOAN_RENEWAL rows | Pure count, and LOAN_RENEWAL already records each renewal | **Update anomaly:** a renewal inserted without incrementing the counter (or rejected after incrementing) lets the max-2 rule (business rules §1) check the wrong number. |
| 15 | **LOAN.Status** domain limited to Active / Returned / Lost; *Overdue* is derived (ReturnedAt IS NULL AND DueAt < now) | Overdue changes with the clock and no row write | A stored 'Overdue' is wrong from midnight until a job runs, so the overdue report (UC-08) and the "no borrowing while overdue" rule would disagree. |
| 16 | Drop **FINE.DaysOverdue**; derive it from LOAN.DueAt and ReturnedAt (or today) | Duplicates LOAN data, and is clock-dependent while the loan is open | **Update anomaly:** correcting a mis-entered ReturnedAt leaves DaysOverdue wrong on the fine. Keep **RatePerDay** and **AmountAssessed**: they snapshot policy at assessment (the rate or cap may change later) and cannot be recomputed from current data. |
| 17 | Drop **FINE.WaivedAmount**; partial reductions come only from approved **FINE_APPEAL.ApprovedReduction**; full waivers use Status = 'Waived' + **WaiverReason** + **WaivedByStaffID** (CHECK ties them together) | WaivedAmount duplicated the sum of approved reductions | **Update anomaly:** an appeal approved or reversed without editing WaivedAmount gives two balances. Business rules §2 require a logged reason for waivers. |
| 18 | **Outstanding balance** is never stored; it is computed from AmountAssessed, approved reductions and completed payments | Pure arithmetic over child rows | Partial payments (UC-05, UC-07) would otherwise need a balance column that goes wrong on any refunded or failed payment. |
| 19 | Drop **FINE_APPEAL.MemberID** | FineID is NOT NULL and FineID → MemberID always: only the fined member may appeal | **Update anomaly:** an appeal could name a different member from its fine. |
| 20 | Keep **FINE.MemberID** and **BOOK_INCIDENT.CopyID / MemberID**, with a trigger checking agreement with the loan | Their parent FKs (LoanID, IncidentID) are optional, so the value is not always reachable by a join | An incident found on the shelf has no loan. Dropping FINE.MemberID would turn the eligibility check into a UNION of two join paths. The trigger closes the anomaly. |
| 21 | Drop **BOOK_INCIDENT.ReplacementCost** and **RepairCharge**; the charge lives only in **FINE.AmountAssessed** (via LOSS/DAMAGE SOURCE), capped against BOOK_COPY.PurchasePrice by procedure | ReplacementCost duplicates BOOK_COPY.PurchasePrice; RepairCharge duplicates the incident's fine | **Update anomaly:** correcting a copy's price or a fine amount leaves the incident showing a different figure. |
| 22 | Drop **BOOK_REVIEW.ModeratedBy**; the moderator is recorded per transition in REVIEW_MODERATION_HISTORY.ModeratorStaffID; add **ModeratedAt** | A review can be moderated more than once (approve → flag → re-decide) | A single column is overwritten, losing earlier moderators, and duplicates the latest history row. A history table with no timestamp cannot be ordered. |
| 23 | **Average rating** and **available-copy count** are not stored on BOOK; they are aggregates in queries or views | UC-09 says "recalculates the average rating"; UC-03 says "reduces the available-copy count" | **Update anomaly:** every review insert, edit, delete or moderation, and every loan or return, would need a synchronised BOOK update. Moderation makes the average depend on Status as well. |
| 24 | **Kept status columns:** BOOK_COPY.Status and FINE.Status stay stored, with a single trigger as owner | Each mixes decision states (Lost, Withdrawn, Waived, Under Appeal) with derivable ones, and UC-07 requires "automatically update the fine status" | The anomaly risk is handled by ownership: only the trigger on LOAN / FINE_PAYMENT / FINE_APPEAL writes these columns, in the same transaction as the source row. This is the Part F trigger. |
| 25 | Drop **APP_USER.PasswordSalt** | Spring Security's BCrypt embeds the salt in the hash string | A separate salt column is a second copy that can disagree with the hash. Addresses the security NFR (encrypted passwords). |
| 26 | **MEMBER.MembershipStatus** loses 'Expired'; expiry is derived from ExpiryDate | Clock-dependent, same reasoning as #15 | Expired membership blocks borrowing (business rules §4) the moment the date passes, not when a batch job runs. |
| 27 | Staff-action FKs retargeted from APP_USER to **STAFF_PROFILE**: LOAN.IssuedByStaffID, plus new ReturnedToStaffID (the diagram's ISSUED/RETURNED BY), FINE_PAYMENT.ReceivedByStaffID, REVIEW_MODERATION_HISTORY.ModeratorStaffID, LOAN_RENEWAL.ApprovedByStaffID | ISSUED/RETURNED BY is drawn from STAFF_PROFILE; Strategy A makes the subtype table a valid FK target | The database refuses a member-only account as issuer or moderator, without a trigger. The unmarked RequestedBy, ApprovedBy and ReceivedBy attributes become real FKs. |
| 28 | Added missing timestamps and decision fields: LOAN_RENEWAL.RequestedAt, FINE_APPEAL.SubmittedAt / DecidedAt / DecidedByStaffID, FINE.AssessedAt, BOOK_INCIDENT.ReportedAt / RecordedByStaffID, FEEDBACK_HISTORY.ChangedByUserID, BOOK_REVIEW.UpdatedAt, MEMBER_FEEDBACK.SubmittedAt / UpdatedAt | UC-06: "record the decision and comments"; UC-08 filter by date; audit trail | Without these, date-range reports (UC-08) and "who decided" are unanswerable. |
| 29 | Cardinality tightened: one overdue FINE per LOAN, one FINE per INCIDENT, one Pending FINE_APPEAL per FINE, one BOOK_REVIEW per (Book, Member), one REVIEW_FLAG per (Review, Member) | Diagram draws these as plain 1:N; business rules and the UCs imply the stricter version | Per-loan fine cap (business rules §2); "edit instead of duplicate" (UC-09); an appeal pauses collection until decided (business rules §2). |
| 30 | **ISA APP_USER → {STAFF_PROFILE, MEMBER} changed from *disjoint*, partial (Part 01) to *overlapping*, partial.** No new object is needed. Nothing now stops one UserID appearing in both STAFF_PROFILE and MEMBER; the UNIQUE UserID in each subtype still keeps each link 1:1. | **Why.** Under disjointness, a library staff member who borrows books needs a second APP_USER account. Email is the AK and the login name, so that account cannot reuse their address. The person would need a second email just to borrow, and would end up with two passwords, two notification inboxes, and loans, fines and audit entries split across two identities the database cannot connect. Disjointness across two tables would also need a trigger; overlap needs none. Partial stays: an account can exist before either profile is created. | **Evidence: no source states outright that library staff borrow books.** Requirements §3 describes the Librarian, Administrator and Finance Officer only by their duties. UC-03 issues books to "the Library Member" without restricting who may be one. "Academic Staff Member" (Requirements §3, UC-04) means teaching staff who hold memberships, not library staff, so it is not evidence either way. The change therefore rests on account identity (one person = one login = one Email AK) and on not imposing a restriction no stakeholder asked for: nothing in the sources forbids staff from borrowing. |
