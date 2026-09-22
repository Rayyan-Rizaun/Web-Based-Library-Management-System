# Library Management System — Business Rules
This file is binding for schema design (D2), stored procedure/trigger logic
(D5), and every feature slice. Where a rule below conflicts with an
assumption made elsewhere, this file wins. Values marked (DEFAULT) were not
specified in the group's source documents — Shashith asked Claude to pick
sensible academic-library values. Confirm with your team/lecturer if any of
these matter for grading, then edit this file and re-run the affected
prompt.

## 1. Borrowing & Loans
- Loan period: 14 days from issue date. (DEFAULT)
- Borrowing limit: 5 active loans for a standard Member; 10 active loans for
  an Academic Staff Member. This is why ACADEMIC_STAFF exists as its own
  subtype on the ER diagram rather than being folded into MEMBER — it
  carries a different limit. (DEFAULT)
- Renewals: maximum 2 renewals per loan. A loan CANNOT be renewed if another
  member has an active reservation on that title. Each renewal extends the
  due date by another 14 days. (DEFAULT)
- A member with any overdue loan cannot borrow additional books until the
  overdue loan is returned and any resulting fine is paid or waived.
  (DEFAULT — standard institutional policy, prevents runaway fines)

## 2. Fines & Payments
- Fine rate: LKR 20 per day overdue, per book. (Sourced from the Finance
  Officer interview in the requirements doc: "LKR 20/day × 5 days = LKR
  100.")
- Fine cap: LKR 500 maximum per individual loan, regardless of how overdue
  it becomes. (DEFAULT — prevents an unbounded fine on a forgotten book)
- Fine statuses: Pending → Partially Paid → Fully Paid, or Waived by a
  librarian/admin with a required reason logged. (Sourced from requirements
  doc — Finance Officer interview explicitly lists these four statuses.)
- Partial payments are allowed and reduce the outstanding balance; the fine
  only moves to Fully Paid when the balance reaches zero. (Sourced from
  requirements doc — sequence diagram for Fine Management shows both a
  full-payment and a partial-payment branch.)
- A member may appeal a fine before paying it. An appeal pauses collection
  until a librarian/admin approves or rejects it. Rejection reinstates the
  original fine; approval reduces or waives it. (Sourced from ER diagram —
  FINE_APPEAL entity — and the 10-use-case scenario doc, UC for fine
  appeals.)

## 3. Reservations
- **Conflict resolved:** any active Member — including Academic Staff — may
  place a reservation. Guest Visitors (mentioned only in the requirements
  doc's stakeholder list, with no APP_USER/MEMBER record on the ER diagram)
  cannot reserve, since reservation requires an authenticated member
  account. (Resolves the "who can reserve" conflict between your documents
  by following the ER diagram's actual entity structure — only APP_USER
  subtypes can act on the system.)
- Reservation window: once a reserved copy becomes available, the member
  has 3 days to collect it before the reservation expires and passes to
  the next member in the queue. (DEFAULT)
- Reservations are queued per title (not per copy) and served in the order
  placed (FIFO). (DEFAULT — standard library queue behaviour, matches the
  RESERVATION entity's relationship to BOOK rather than BOOK_COPY on your
  ER diagram)

## 4. Membership
- Member types: Student Member and Academic Staff Member, both subtypes of
  MEMBER (matching the ER diagram's ISA hierarchy). Rules differ only in
  borrowing limit (see Section 1) — loan period, fine rate and renewal
  count are the same for both. (DEFAULT)
- Membership validity: one academic year, renewed automatically at the
  start of each new academic year for continuing students/staff. An
  expired membership blocks new borrowing and reservations but does not
  affect returning already-borrowed books or paying an existing fine.
  (DEFAULT)

## 5. Damage & Loss
- Lost book: member is charged that specific copy's replacement cost —
  `BOOK_COPY.PurchasePrice`, not a title-level price — plus a flat
  LKR 500 processing fee. (DEFAULT. Per-copy rather than per-title is
  deliberate: copies of the same title bought in different years have
  different purchase prices, and charging the actual cost of the copy
  lost is both fairer and easier to defend than an averaged figure.)
- Damaged book: charge is set case-by-case by a librarian at the time the
  incident is logged (using the BOOK_INCIDENT entity), capped at that
  copy's `PurchasePrice`. There is no fixed damage-fee formula — the
  librarian records a justification note with the charge. (DEFAULT —
  matches your ER diagram's BOOK_INCIDENT design, which stores an
  incident description rather than a fixed fee table)
- Both lost and damaged charges can be appealed through the same
  FINE_APPEAL flow as an overdue fine.

## 6. Reviews & Feedback
- **Conflict resolved:** reviews are moderated. A submitted review enters a
  "Pending" state and is not visible to other members until a Library
  Administrator approves it. The Administrator may reject a review with a
  reason, which is logged in REVIEW_MODERATION_HISTORY. (This follows the
  explicit statement in the Assignment01 Requirements doc — the Library
  Member stakeholder interview states outright: "The Library Administrator
  should be able to: View all submitted reviews, Approve reviews before
  publication, Reject inappropriate reviews." Where UC-09's use-case flow
  implied immediate publish, this file overrides it — moderation is also
  the safer, more defensible build for a viva, since it exercises more of
  the schema you already designed for it: BOOK_REVIEW, REVIEW_FLAG,
  REVIEW_MODERATION_HISTORY.)
- Any published review can subsequently be flagged by another member for
  re-review (REVIEW_FLAG); a flagged review is hidden pending
  administrator re-decision.
- Member feedback (separate from reviews) follows the status flow already
  defined in your requirements doc: Submitted → Under Review → In Progress
  → Resolved → Closed.

## 7. Notes for D2 (DDL)
- Every numeric value above (14 days, LKR 20/day, 5/10 loan limit, 2
  renewals, 3-day reservation window, LKR 500 fine cap, LKR 500 processing
  fee) should be implemented as named constants or a row in SYSTEM_SETTING
  — not hard-coded into a stored procedure — so a lecturer's question like
  "what if the loan period changed to 21 days" has a one-row answer, not a
  code change.
- Anything in this file marked (DEFAULT) is a judgement call, not a
  requirement from your source documents. Say so plainly if asked in
  viva: "our source documents didn't specify this, so we set a reasonable
  policy and made it configurable."

## 8. Authentication & Access (UC-01)
- Account lockout: an account is locked after 5 failed login attempts for
  its email within a rolling 15-minute window (both figures given directly
  by the group, not defaults). A locked account (`AppUser.Status = 'Locked'`)
  stays locked until an administrator or librarian reactivates it — there is
  no scheduled job in this project to auto-unlock accounts after a cool-down,
  so an explicit staff action is the only way out. (DEFAULT — "how a lock
  ends" was not specified; leaving it to require deliberate staff action is
  the safer default for a library system, and the alternative — a timed
  auto-unlock — would need a scheduler this project does not otherwise have.)
- Every login attempt against an existing OR non-existing email is recorded
  in FAILED_LOGIN_ATTEMPT with one of the four FailureReason values already
  in the schema (UnknownEmail, BadPassword, AccountLocked,
  AccountDeactivated); only BadPassword attempts against a real, not-yet-
  locked account count toward the 5-in-15-minutes threshold above — an
  UnknownEmail attempt has no account to lock, and an attempt already
  rejected as AccountLocked/AccountDeactivated does not need to lock the
  account a second time. (DEFAULT — which reasons count toward lockout was
  not specified.)
- Session timeout: 30 minutes of inactivity. (DEFAULT)
- Password reset link validity: 30 minutes, single use. Requesting the
  email logs the raw token to the application console rather than sending
  real mail, per this phase's explicit scope — see
  `com.lms.user.PasswordResetService`. (DEFAULT for the expiry; console
  logging instead of mail is this task's own instruction, not a judgement
  call.)
- "Remember me" is off: every session ends at browser close or the 30-minute
  idle timeout above, whichever comes first. There is no persistent login
  cookie. (This task's own instruction, not a default.)
- **Self-registration (backlog item PB-21).** A guest submitting the public
  registration form creates an `AppUser` (Status `Active` — they can log in
  immediately and check their status) and a `Member` row with
  `MembershipStatus = 'Suspended'`. (DEFAULT and a deliberate repurposing:
  the schema's `MembershipStatus` enum is `Active | Suspended | Cancelled`
  — there is no `Pending` value, and adding one is a schema change this
  phase does not make. `Suspended` already means exactly what a pending
  self-registration needs functionally: the member exists and can sign in,
  but every borrowing/reservation rule that checks `MembershipStatus =
  'Active'` already refuses them, with no new rule required. An
  administrator or librarian reviews the registration and flips
  `MembershipStatus` to `Active` to approve it — that approval screen is
  UC-01 territory but is not built in this pass; until it exists, changing
  a pending member's status is a manual UPDATE. The self-registration
  service grants **no** `UserRole` row at all, for either the "Library
  Member" or "Academic Staff Member" role — a guest cannot assign
  themselves a role under any circumstance, and role assignment stays a
  separate, staff-only action from membership approval.)
- **Role names as Spring authorities.** `Role.RoleName` values (e.g.
  "Library Administrator", "Librarian") are granted to the security
  principal verbatim as `GrantedAuthority` strings, with no `ROLE_` prefix
  and no case change. Every check uses `hasAuthority('Exact Role Name')`,
  never `hasRole(...)` — Spring's `hasRole('X')` silently looks for
  `'ROLE_' + X`, which would need a second, parallel naming scheme (e.g.
  `ROLE_LIBRARY_ADMINISTRATOR`) kept in step with the `Role` table by hand.
  Using the database value directly means there is exactly one spelling of
  each role, in one place, and it is the one already in `dbo.Role`.
