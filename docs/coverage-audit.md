# Implementation Coverage Audit — UC-01 through UC-10

Graded against `docs/scenarios.pdf` (all ten use cases), `docs/business-rules.md`,
and `docs/CLAUDE.md`, evidence taken only from what is actually in
`src/main/java/com/lms/` and `src/main/resources/templates/`. A step is marked
BUILT only where the exact controller/service method and, where relevant, the
exact template line were read. "PARTIAL" means the mechanism exists but does
not do everything the flow describes. "MISSING" means no code path reaches it
at all. Notification writes, `AuditLog` rows, history rows and status
transitions are called out explicitly wherever the scenario implies one,
because these are the easiest thing to build a screen around without actually
writing the row.

---

## UC-01: Manage User Accounts and Roles (`com.lms.user`)

### Main flow — Administrator manages accounts
1. Administrator selects "User Management" → account list — **MISSING**. No `UserController`/admin list route exists anywhere; `components/sidebar.html` has no such entry.
2. Administrator selects an existing user or creates a new one — **MISSING**.
3. Administrator enters/updates user information — **MISSING**.
4. Administrator assigns a role (Library Member, Librarian, Finance Officer, Library Administrator) — **MISSING**. `UserRoleRepository` exists in `com.lms.common.domain` but is never called from any application code; the only reference to it anywhere is a javadoc comment in `RegistrationService.java` explaining that self-registration deliberately does *not* use it.
5. Administrator saves; system validates; creates/updates account; success message — **MISSING** (nothing to save to).

### Main flow — Self-service registration and profile update
6. Guest registers → `AppUser` Active + `Member` Suspended, no role granted — **BUILT**. `RegistrationController.java` → `RegistrationService.register()`: sets `AppUserStatus.Active` and `MembershipStatus.Suspended`, never touches `UserRoleRepository`, matches `business-rules.md` §8 exactly. `@AuditAction(action="CREATE", entity="AppUser")` on `register()`.
7. Self-service profile update ("My Profile" → edit → save → validate → confirm) — **MISSING**. `AccountController.java` only exposes `GET /account`; `templates/user/account.html` is entirely read-only (`th:text` throughout, no `<form>`). No `POST /account`, no edit DTO.

### Alternative flows
- Required info missing → system requests completion — **PARTIAL**. Built for registration (`RegistrationForm` Bean Validation + inline errors). Cannot apply to admin create/update (doesn't exist) or profile update (doesn't exist).
- Duplicate username/email → error — **PARTIAL**. Built for registration (`RegistrationService` checks email and national ID, throws `DuplicateRegistrationException`, shown as inline field error). No admin-side duplicate check exists because admin create doesn't exist.
- Administrator deactivates account → login prevented — **PARTIAL**. The *consequence* is fully built (`AppUserPrincipal.isEnabled()` returns false for `Deactivated`, `LoginFailureHandler` maps it to `AccountDeactivated`, shown on the login page) — but no code anywhere ever sets `AppUserStatus.Deactivated`, because the deactivate action itself doesn't exist.
- Member/Librarian cannot change their own role — **MISSING**. Vacuously true only because no one can change any role at all; there is no positive guard distinguishing "change your own role" from "change someone else's."

### Business-rules.md §8 items not in the scenario's own flow list, checked because they are binding
- Account lockout, 5 failures / 15 min — **BUILT**. `LoginAttemptService.recordFailure()`/`lockIfThresholdReached()`, thresholds read from `SystemSetting` (`Security.LockoutMaxAttempts`, `Security.LockoutWindowMinutes`), only `BadPassword` counts toward the threshold, matching the rule precisely. No auto-unlock exists anywhere (matches "explicit staff action is the only way out") — but per the gap above, there is also no staff action to *perform* that unlock.
- `FailedLoginAttempt` logged with all 4 `FailureReason` values — **BUILT**. `LoginFailureHandler.reasonFor()` maps every Spring Security exception to `UnknownEmail`/`BadPassword`/`AccountLocked`/`AccountDeactivated`; every attempt is logged regardless of reason (no FK to `AppUser`, by design).
- Session timeout, 30 min idle — **BUILT**. `SecurityConfig.sessionTimeoutCustomizer()` sets `Duration.ofMinutes(30)`. No `rememberMe()` call anywhere, matching "no persistent login cookie."
- Password reset (console-logged token, 30-min validity, single-use) — **BUILT**. `PasswordResetService`: raw token only in the log (`PasswordResetMailer.log.info`), only its SHA-256 hash persisted, `Security.PasswordResetTokenMinutes` read from `SystemSetting`, `ConsumedAt` set on use and filtered out of future lookups. `@AuditAction(action="UPDATE", entity="AppUser")` on `resetPassword()`.
- `hasAuthority('Exact Role Name')`, never `hasRole(...)` — **BUILT**. Confirmed project-wide: zero occurrences of `hasRole(` anywhere in `src/main/java`.

### AuditLog check
Only two `@AuditAction` usages in `com.lms.user`: `RegistrationService.register()` (CREATE/AppUser) and `PasswordResetService.resetPassword()` (UPDATE/AppUser). Nothing else is auditable because nothing else exists.

### Deviations / extra behavior
- `RegistrationService` forces `MemberType.Student` on every self-registration — not stated anywhere in UC-01 or business-rules.md §8, but doesn't contradict either.

---

## UC-02: Manage Book Catalog and Inventory (`com.lms.catalogue`)

### Main flow
1. Librarian selects "Book Catalog and Inventory Management" → catalog list — **BUILT**, under the label "Books" (`components/sidebar.html`), not the scenario's literal phrase — cosmetic only. `BookController.list()` → `GET /catalogue/books`.
2. Librarian selects "Add New Book" — **BUILT**. `BookController.newForm()`, staff-gated.
3. Librarian enters title, author, category, ISBN, **number of copies**, other details — **PARTIAL**. `BookForm` captures title, one primary author, categories, ISBN-13/10, subtitle, publisher, year, edition, language, description, keywords — but has **no copy-count field at all**. Copies are created only afterward, one at a time, on a completely separate screen (`BookCopyController`, reached via "Add copy" on the book's own detail page). A newly created book has zero copies until a librarian does this separately.
4. Librarian saves; system validates — **BUILT**. Full Bean Validation on `BookForm`, inline errors on failure.
5. System adds the book and sets availability status — **PARTIAL**, downstream of #3: `Book` correctly has no stored availability column (it's computed live from `BookCopy.Status`), but since a new book starts with zero copies, "sets the availability status" in practice means "shows 0 of 0 available" until copies are separately added.
6. System displays a confirmation message — **BUILT**. Flash toast on the redirect to the new book's detail page.

### Alternative flows
- Search by title/author/category before adding — **BUILT**, and extended: also matches ISBN-13, ISBN-10, and keyword in the same query (`BookRepository.search`), self-documented as a deliberate extension.
- Duplicate ISBN → error — **BUILT**. `BookService.rejectDuplicateIsbn()` checks both ISBN-13 and ISBN-10, shown as an inline field error via `DuplicateFieldException`.
- Update book details — **BUILT**. Same validation/duplicate handling as create.
- Delete a record with no active borrowing transactions — **BUILT**, as a soft-deactivate (never a row delete, to preserve loan/review history): `BookService.deactivate()` checks `loans.existsByCopyBookBookIdAndStatus(bookId, Active)` and throws `ActiveLoanException`, shown as a flash toast. The identical guard exists at copy level in `BookCopyService.withdraw()`.
- Library Administrator can also access this screen — **BUILT**. Every write method on every catalogue service is gated `hasAuthority('Librarian') or hasAuthority('Library Administrator')` identically.

### Postcondition
- Members and guest visitors can search/view latest availability without logging in — **BUILT**, confirmed at `SecurityConfig`: `GET /catalogue/books` and `GET /catalogue/books/{id}` are explicitly `permitAll()`; everything else falls under `anyRequest().authenticated()`.

### Deviations / extra behavior
- `AuthorController`/`CategoryController`/`PublisherController` list `GET`s carry no `@PreAuthorize` of their own — access control relies entirely on the underlying service method's `@PreAuthorize`. Works today, but is an inconsistent defense-in-depth pattern next to `BookController.newForm()`, which explicitly self-guards for exactly this reason (see that method's own comment).
- `BookCopyService.withdraw()` also blocks withdrawing a copy that is `OnHold` for a reservation — a UC-04 concept UC-02's own text never mentions; reasonable, not a contradiction.

### AuditLog check
**Zero** `@AuditAction` usages anywhere in `com.lms.catalogue`. Book, copy, author, category and publisher creation, edits and deactivations leave no audit trail at all, unlike every other feature package in this codebase.

---

## UC-03: Borrow and Return Books (`com.lms.borrowing`)

### Main flow — Borrow
1. Librarian selects "Issue Book" — **BUILT**. `LoanController.issueForm()` → `GET /borrowing/issue`.
2. Librarian searches for and selects the member — **BUILT**. `BorrowingService.searchMembers`/`eligibility`.
3. System displays the member's borrowing eligibility — **BUILT**. `MemberEligibilityRow` shows active-loan count vs. limit, membership status, and the exact refusal reason if any.
4. Librarian searches for and selects an available copy — **BUILT**. `BorrowingService.searchCopies` (barcode-exact first, else title match across copies).
5. System checks book availability — **BUILT**, inside `issue()`: reference-only and non-Available copies are both rejected.
6. System calculates the due date — **BUILT**. `BorrowedAt + SystemSetting('Loan.PeriodDays')`.
7. Librarian confirms the transaction — **BUILT**. `LoanController.issue()` POST.
8. System records the borrowing transaction — **BUILT**.
9. System changes the copy status to unavailable — **BUILT**. `copy.setStatus(BookCopyStatus.OnLoan)`.
10. System displays due date and confirmation — **BUILT**. Flash toast with the due date.

### Main flow — Return
11. Librarian selects "Return Book" — **BUILT**. Return action on each row of Current Loans.
12. Librarian enters/searches the borrowing record — **BUILT**, implicitly (the row is already on screen; no separate lookup-by-ID search exists, but every active loan is already listed).
13. System displays the borrowed book and due date — **BUILT**.
14. Librarian confirms the return — **BUILT**, via a hand-built modal asking the return condition.
15. System calculates an overdue fine if applicable — **BUILT**. `days × Fine.RatePerDay`, capped at `Fine.MaxPerLoan`, both read from `SystemSetting`.
16. System updates book availability — **BUILT**, with a documented deviation: a `Damaged` return sets the copy to `Damaged`, not `Available`, and files a `BookIncident` — correct per business-rules.md §5, but the return-condition list itself only offers `Good/Fair/Poor/Damaged`, not the scenario's literal "New" (schema's `CK_Loan_ReturnCondition` does not allow "New"; flagged in the code's own javadoc, not silently dropped).
17. System records the return transaction — **BUILT**. `@AuditAction(action="RETURN", entity="Loan")`.
18. System displays confirmation and fine amount — **BUILT**.

### Main flow — Renew
19. Member selects "Renew Book" — **BUILT**. `MyLoanController.renew()`.
20. System displays the current loan and due date — **BUILT** (My Loans list).
21. System checks for a competing reservation — **BUILT**. `applyRenewal()` checks `ReservationRepository.existsByBookBookIdAndMemberMemberIdNotAndStatusIn(..., [Waiting, Ready])`.
22. If no one is waiting, extends the due date — **BUILT**. `+SystemSetting('Renewal.ExtensionDays')`, capped at `SystemSetting('Renewal.MaxPerLoan')` approved renewals (counted from `LoanRenewal` rows, never a stored counter).
23. System updates the borrowing record and displays the new due date — **BUILT**. `@AuditAction(action="RENEW", entity="LoanRenewal")` on both the staff and member renewal paths.

### Alternative flows
- Book unavailable → issuing refused — **BUILT**.
- Outstanding restrictions → borrowing prevented — **BUILT**: expired/non-Active membership, borrowing limit, and any existing overdue loan are all checked before issue.
- Fine after a return "sent to Fine Management" — **BUILT** functionally: the `Fine` row is created directly in the same table `com.lms.fine`'s screens already read from, so it appears there immediately; there is no separate "send" step because none is needed.
- Reservation blocks renewal — **BUILT** (see #21).

### AuditLog check
`returnBook()` and both renewal paths are audited. **`issue()` itself carries no `@AuditAction`** — the single most consequential write in this whole package (a new `Loan` row, a copy going out of circulation) leaves no audit row. Every other state-changing method in this class is audited; this one specifically is not.

### Notification check
No code in `com.lms.borrowing` ever writes to `NotificationRepository`. `NotificationType.DueSoon`, `.Overdue` and `.RenewalDecision` all exist in the enum and are never used by any class in the codebase (confirmed by a project-wide grep) — there is no scheduled job anywhere (`@Scheduled` appears nowhere in `src/main/java`) that would ever raise a due-soon or overdue reminder in the first place.

---

## UC-04: Manage Book Reservations (`com.lms.reservation`)

### Main flow
1. Member searches for a book — **BUILT** (catalogue search, out of package).
2. System displays the book is unavailable — **BUILT**. `ReservationService.panelFor()`, called directly from `catalogue/book-detail.html`.
3. Member selects "Reserve Book" — **BUILT**. Inline form on the book detail page, `POST /reservations`.
4. System checks for an existing active reservation by this member — **BUILT**. `reserve()` checks `existsByBookBookIdAndMemberMemberIdAndStatusIn`.
5. System adds the member to the waiting list — **BUILT**.
6. System displays reservation status and queue position — **BUILT**. `queuePositionFor()` recomputed live (never stored, per R12) on both `panelFor` and "My Reservations".
7. When a copy becomes available, system notifies the next eligible member — **BUILT**. `markReady()` writes a `Notification` (`NotificationType.ReservationReady`) with the hold's expiry date.
8. Librarian confirms allocation on collection — **BUILT**. `fulfill()` → status `Fulfilled`.

### Alternative flows
- Book available → suggest borrowing instead — **BUILT**. `reserve()` refuses with exactly this message when an available, non-reference copy exists.
- Already reserved same book → error — **BUILT** (#4 above).
- Member may cancel before allocation — **BUILT**. `cancel()`, member-owned, `Waiting`/`Ready` only.
- Uncollected hold past `ExpiresAt` → system cancels it and the next member becomes eligible — **PARTIAL**. The mechanism exists (`expire()`, staff-only, re-checks the hold is actually past `ExpiresAt`) and "next member eligible" needs no extra code (queue position is recomputed live) — but nothing *automatically* detects an expired hold. A librarian must notice and click "Expire hold" by hand; there is no scheduled job. The scenario's own wording ("the system cancels the reservation") implies automatic detection this build does not have.

### Staff-only extension beyond the scenario's literal text
- "Cancel" with a required reason, on any open reservation — **BUILT**. `cancelByStaff()`: writes a `Notification` (`NotificationType.General`, since no more specific type fits) telling the member why, and a hand-built `AuditLog` row (`ActionName="CANCEL"`, `EntityName="Reservation"`, `Details` as JSON via `ObjectMapper`, since `Reservation` has no column to hold a free-text reason and `@AuditAction`'s aspect cannot carry one).

### AuditLog / Notification check
`markReady` and `cancelByStaff` both notify correctly. `fulfill()` and `expire()` write **no** `Notification` and **no** `AuditLog` row at all — a member is never told their reservation was fulfilled or that their hold expired, and neither staff action leaves an audit trail (only `cancelByStaff`'s hand-built one does).

---

## UC-05: Manage Fines and Fine Payments (`com.lms.fine`)

### Main flow
1. Finance Officer selects "Fine Management" — **BUILT**. `FineController.outstanding()` → `GET /fines`.
2. System displays outstanding fine records — **BUILT**, plus three stat tiles (total outstanding, collected this month, open appeals) beyond what the scenario asks for.
3. Finance Officer searches for a member or selects a fine — **BUILT**. Search/filter/sort/paginate.
4. System displays borrowing details, overdue days, calculated fine amount, and payment status — **PARTIAL**. `FineDetailView` (the staff pay-screen header, `com/lms/fine/dto/FineDetailView.java`) carries `fineType`, `amountAssessed`, `amountPaid`, `balance`, `status` — but **no days-overdue field at all**. The member's own equivalent (`MyFineDetailView`) does compute and show days overdue; the staff screen a Finance Officer actually uses to "verify the fine amount" does not.
5. Finance Officer verifies the fine amount — **PARTIAL**, limited by #4 (balance and assessed amount are visible; the days×rate breakdown that would let someone actually verify the arithmetic is not, on the staff side).
6. Member makes the payment / Finance Officer records it — **BUILT**. `FineController.pay()` → `FineService.pay()`.
7. System updates payment status to paid/partially paid — **BUILT**. `recomputeStatus`-equivalent logic inline in `pay()`.
8. System stores the payment transaction — **BUILT**. `FinePayment` row, unique receipt number.
9. System displays payment confirmation — **BUILT**. Receipt page via flash attribute.

### Alternative flows
- No unpaid fine exists → "No outstanding fines" — **BUILT**. `components/empty-state` shown when `rows` is empty (`fine/outstanding.html`).
- Payment less than fine amount → partial payment, balance retained — **BUILT**. `pay()` computes `newBalance` and sets `PartiallyPaid` unless it reaches exactly zero.
- Calculation incorrect → Finance Officer refers the record for correction before accepting payment — **MISSING**, distinct from UC-06's now-built appeal referral (see UC-06 below). This flow describes a Finance Officer unilaterally flagging *any* fine as wrong while reviewing it for payment — no appeal need exist yet. No such action exists anywhere on the Outstanding Fines or Pay screens (`fine/outstanding.html`, `fine/pay.html` offer only Pay and Waive). The appeal-referral feature just built (`FineService.referAppealForCorrection`) only operates on an *existing Pending appeal* and is reachable only from the Fine Appeals queue, not from the payment screen.

### AuditLog check
`pay()` is `@AuditAction(action="PAYMENT", entity="Fine")`; `waive()` is `@AuditAction(action="WAIVE", entity="Fine")`. Both correctly audited.

---

## UC-06: Manage Fine Appeal and Waiver (`com.lms.fine`)

### Main flow
1. Member selects "Appeal Fine" for a specific fine — **BUILT**. `MyFineController.appeal()`, gated to `Pending` fines only.
2. Member enters the reason — **BUILT**. `AppealForm`.
3. Member submits the appeal — **BUILT**.
4. System records the appeal as Pending — **BUILT**. `submitAppeal()` also sets `Fine.Status = UnderAppeal`, correctly blocking payment while it's open (`pay()` explicitly refuses `UnderAppeal` fines).
5. Administrator (in consultation with Finance Officer) reviews the appeal — **BUILT** as a shared queue (`fine/appeals.html`), reachable by all three staff roles (`Finance Officer`, `Librarian`, `Library Administrator`) per `FineService.STAFF_ROLES` — broader than "Administrator decides," see deviation note below.
6. Administrator approves or rejects — **BUILT**. `approveAppeal()`/`rejectAppeal()`.
7. If approved, system reduces or waives the fine — **BUILT**. Reduction capped at the outstanding balance; a reduction that zeroes the balance lands on `FullyPaid` via `recomputeStatus()`.
8. System records the decision and comments — **BUILT**. `DecidedBy`, `DecidedAt`, `DecisionComments` (reject only) / `ApprovedReduction` (approve only), matching `CK_FineAppeal_DecisionMatchesStatus` exactly.
9. System updates the appeal and fine status — **BUILT**.
10. System notifies the member of the outcome — **BUILT**. `approveAppeal`/`rejectAppeal` both call a shared `notify()` helper writing a `Notification` (`NotificationType.AppealDecision`) stating the outcome and the resulting fine balance (verified live: approve → "…this fine now stands at LKR 200.00."; reject → "…remains payable in full at LKR 200.00.").

### Alternative flows
- Appeal rejected → fine payable in full — **BUILT** (#10 above; `recomputeStatus` leaves the balance untouched by a rejection).
- Finance Officer refers a disputed calculation for correction before a decision — **BUILT**. `FineService.referAppealForCorrection()`, gated `hasAuthority('Finance Officer')` specifically (not the broader `STAFF_ROLES`); appeal stays `Pending`; the referral note (with the referring officer's name, since `DecidedByStaffID` must stay `NULL` while `Pending` per `CK_FineAppeal_DecisionMatchesStatus` — there is no schema column to hold "who referred" on an undecided appeal, so the note is carried as text in the existing, otherwise-unused-while-Pending `DecisionComments` column) is shown in the queue as "Referred for correction — by \<name\>: \<note\>" (`fine/appeals.html`). Approving afterward explicitly clears the stale note (`appeal.setDecisionComments(null)` inside `approveAppeal()`) so it never leaks into a final decision.
- Member may view appeal status at any time — **BUILT**. `fine/detail.html` always shows the most recent appeal's status.

### Deviation
UC-06 names "Library Administrator" as the decision-maker throughout; the actual `@PreAuthorize` on `approveAppeal`/`rejectAppeal` is the broader `STAFF_ROLES` (Finance Officer, Librarian, *or* Library Administrator) — any of the three can decide an appeal, not just an Administrator. Not flagged as wrong (business-rules.md doesn't name a narrower role), but worth noting as broader access than the scenario text implies.

### AuditLog check
`approveAppeal`, `rejectAppeal` and `referAppealForCorrection` are all `@AuditAction`-annotated (`APPROVE`/`REJECT`/`REFER`, entity `FineAppeal`). Fully covered.

---

## UC-07: Manage Fine Payment and Receipt (`com.lms.fine`)

### Main flow
1. Member selects "Pay Fine" — **MISSING**. There is no member-facing payment route anywhere. `MyFineController` exposes exactly three actions: `myFines`, `detail`, `appeal` — no `pay`. `templates/fine/detail.html` (the member's own fine page) has an "Appeal this fine" button and nothing else; no Pay button, no payment form.
2. System displays the outstanding fine amount — **BUILT** (on the same read-only detail page).
3. Member selects a payment method — **MISSING** (no form to select one in).
4. Member enters the payment amount — **MISSING**.
5. Member confirms the payment — **MISSING**.
6. System processes the payment — **MISSING** for a member-initiated payment. The only implemented payment path is `FineController.pay()`, staff-only (`STAFF_ROLES`), under `/fines/{id}/pay` — a Finance Officer records the payment, not the member. `PaymentMethod` legally includes `Online` (`CK_FinePayment_PaymentMethod`), implying self-service online payment was intended by the schema, but no controller reaches it from the member side.
7. System calculates the remaining balance / updates status — **BUILT**, inside the staff-only `pay()`.
8. System generates a unique receipt — **BUILT**. `generateUniqueReceiptNumber()`.
9. System displays the receipt and updated balance — **PARTIAL**. Works once, immediately after a staff-recorded payment, via a flash attribute (`FineController.receipt()` explicitly refuses a second view: "That receipt has already been shown once."). There is no member-facing receipt view at all.

### Alternative flows
- Finance Officer may record a payment on behalf of the member (cash at counter) — **BUILT**. `CK_FinePayment_CashHasReceiver` enforced (`Cash` requires `ReceivedByStaffID`), the only path this codebase actually has is this one.
- Payment amount less than fine → partial payment retained — **BUILT** (shared logic with UC-05).
- Member may view past payment history and reprint previous receipts at any time — **PARTIAL**. Payment *history* (receipt number, method, amount, date) is listed as a plain table row on `fine/detail.html` for every completed payment — but there is no link, route, or button to re-view/reprint any specific past receipt as a formatted receipt. The only receipt-rendering template (`fine/receipt.html`) is reachable exclusively through the one-time staff flash-attribute flow described above.

### What this means in practice
Every member-facing "pay my own fine" capability UC-07 describes as the primary actor's own main flow does not exist in this build — the entire feature, as implemented, is staff-mediated only (this is functionally UC-05's Finance Officer recording flow reused, not a distinct member self-service path).

---

## UC-08: Generate Reports and Analytics (`com.lms.report`)

**The package does not exist.** No `com.lms.report` anywhere under `src/main/java/com/lms/`, no `templates/report/`, no `/report*` route anywhere in the codebase.

### Main flow — all MISSING
1. Administrator/Librarian selects "Reports and Analytics" — **MISSING**. `components/sidebar.html` renders a "Reports" entry (`th:href` absent, literal `href="#"`) — a dead link.
2. System displays available report types — **MISSING**.
3. User selects a report type — **MISSING**. The only artifact is the `ReportType` enum (`com.lms.common.domain.ReportType`, 16 constants matching `CK_ReportAudit_ReportType`) — declared, never read by any class.
4. User selects a date range/filter — **MISSING**.
5. System retrieves data and generates the report — **MISSING**. No `ReportService` or equivalent exists.
6. System displays totals and details — **MISSING**.
7. User downloads or prints — **MISSING**.

### Alternative flows — all MISSING
- No records match → empty-report message — **MISSING**.
- Invalid date range → system requests a valid range — **MISSING**.
- User changes filters and regenerates — **MISSING**.

### ReportAudit check
`ReportAudit`, `ReportAuditRepository`, and `ReportType` all exist in `com.lms.common.domain` — pure schema/entity scaffolding. `grep`-confirmed: zero calls to `reportAuditRepository.save(...)` or `new ReportAudit()` anywhere in the codebase. Dead code, not a partially-built feature.

### Note on `home.html`
The dashboard's stat tiles and activity table (`HomeController`, `templates/home.html`) could be mistaken for a report at a glance. Both are explicitly self-documented in their own comments as static demo/shell content: `HomeController`'s `ACTIVITY` list and its four stat tiles are hardcoded literals, not database queries, and the class javadoc says so outright. Not part of UC-08 by any reasonable reading.

---

## UC-09: Manage Book Ratings and Reviews (`com.lms.review`)

Business-rules.md §6 overrides this use case's literal "immediate publish" wording with mandatory moderation — the implementation correctly follows the binding rule, not the scenario text, and documents this in `com.lms.common.domain.BookReview`'s own comments and `00_relational_mapping.md`'s conflict table. Every "MISSING/PARTIAL" mark below is against the *moderated* version of the flow, not the scenario's literal immediate-publish description.

### Main flow
1. Member selects a previously borrowed book — **BUILT** (catalogue navigation).
2. Member selects "Add Rating and Review" — **BUILT**. Inline form directly on `catalogue/book-detail.html`, injected via `@reviewService.panelFor(...)`, the same direct-bean-call pattern UC-04 established.
3. Member enters rating and text — **BUILT**. `ReviewForm` (1–5 rating, optional text).
4. Member submits — **BUILT**.
5. System validates — **BUILT**: refuses a review with no prior loan of the book (`loans.existsByMemberMemberIdAndCopyBookBookId`), refuses a second review of the same book (edits the existing one instead, per `UQ_BookReview_BookID_MemberID`).
6. System saves the rating and review — **BUILT**, `Status = Pending` (per business-rules §6, not the scenario's "displays on the catalog page" — a Pending review is invisible to everyone but its own author, correctly).
7. System displays the review on the book's catalog page — **PARTIAL by design**: only once `Approved`. The member's own Pending review is shown back to them, clearly marked, while nobody else sees it — this is the moderation override working as intended, not a bug.

### Alternative flows
- Member may edit or delete their own review at any time — **BUILT**, no status restriction (matches "at any time" literally): `edit()` resets to `Pending` and re-queues for moderation; `remove()` sets `Status = Removed` (never a hard delete), guarded only against removing an already-Removed row.
- Not previously borrowed → submission refused — **BUILT** (#5 above).
- Library Administrator may remove an inappropriate review — **PARTIAL**. The only paths to `Removed` are (a) the member's own `remove()`, or (b) staff `uphold()` on an *existing flag* after a member has already reported it. There is **no direct "Remove" action available to an Administrator on an Approved review that nobody has flagged yet** — `review/moderation-detail.html` shows Approve/Reject only while `Pending` and Uphold/Dismiss only while `Hidden`; nothing is shown for an `Approved` review. An administrator who personally notices an inappropriate published review has no button to act on it without first waiting for a member to flag it.

### Extension beyond the scenario — the flag/moderation pipeline
- A member can flag another member's Approved review, moving it to `Hidden` automatically — **BUILT**. `flag()`, blocked against self-flagging and double-flagging (`existsByReviewIdAndFlagsReportedByMemberId`), writes a `ReviewModerationHistory` row with `ModeratorStaffID = NULL` — the one schema-sanctioned exception (`CK_ReviewModerationHistory_ModeratorRequired`) for an automatic, staff-less transition.
- Staff uphold (→ `Removed`) or dismiss (→ `Approved`) a flag — **BUILT**, each resolving every `Open` `ReviewFlag` on that review and writing a `ReviewModerationHistory` row with the real moderator.
- Reject requires a reason — **BUILT** (`CK_ReviewModerationHistory_RejectHasReason` enforced in `reject()`).

### AuditLog / history check
Every one of `submit`/`edit`/`remove`/`flag`/`approve`/`reject`/`uphold`/`dismiss` carries its own `@AuditAction`. `ReviewModerationHistory` rows are written correctly for every staff decision and the automatic flag-triggered hide. **One documented, deliberate exception**: `edit()` resets an already-`Approved`/`Rejected` review to `Pending` but writes **no** `ReviewModerationHistory` row for that transition — `CK_ReviewModerationHistory_ModeratorRequired` requires a real `ModeratorStaffID` for every transition except the flag-triggered `Hidden` one, and a member's own edit has no staff moderator to attribute it to. There is no schema-legal way to log this transition without a schema change, which this task does not make.

### Notification check
No code in `com.lms.review` ever writes to `NotificationRepository`. `NotificationType.ReviewDecision` exists in the enum and is never used anywhere in the codebase.

---

## UC-10: Manage Member Feedback (`com.lms.feedback`)

Business-rules.md §6 also overrides this scenario's looser "Pending/Reviewed/Resolved" wording with the real five-state lifecycle (`Submitted → Under Review → In Progress → Resolved → Closed`) — correctly followed, documented deviation.

### Main flow
1. Member selects "Submit Feedback" — **BUILT**.
2. Member enters feedback details (category, subject, description, priority) — **BUILT**. `FeedbackForm`.
3. Member submits — **BUILT**.
4. System records the feedback as Submitted, with a generated unique reference — **BUILT**. `generateUniqueReference()` ("FB-\<year\>-\<6 digits\>").
5. Administrator reviews the submitted feedback — **BUILT**. `feedback/manage-list.html` + `feedback/review.html`, staff-gated to `Library Administrator` specifically (narrower than the shared `STAFF_ROLE` pattern elsewhere — correctly matches UC-10's own "Supporting Actor: Library Administrator" only).
6. Administrator responds to the feedback — **BUILT**. `review()`.
7. System updates the feedback status — **BUILT**, with a written `FeedbackHistory` row (previous/new status, previous/new response, who, when) — verified: `review()` builds and appends a `FeedbackHistory` row every time either the status or the response actually changes (guarded: "Nothing to update" thrown if neither changed).
8. **System notifies the member of the response — MISSING.** No code in `com.lms.feedback` ever writes to `NotificationRepository` — confirmed by a project-wide grep for `Notification` (only `com.lms.fine` and `com.lms.reservation` import it). This is an explicit, literal main-flow step in UC-10's own text ("The system notifies the Library Member of the response") that is simply not implemented. `NotificationType.FeedbackResponse` exists in the enum for exactly this purpose and is never used anywhere.

### Alternative flows
- Member may view or edit their own feedback before it is reviewed — **BUILT**. `edit()`/`forEdit()`, gated to `Status == Submitted && AdminResponse == null` exactly (`requireEditable()`), enforced identically for `withdraw()`.
- Required info missing → system requests completion — **BUILT**. Bean Validation on `FeedbackForm`.
- Administrator may mark feedback as low priority — **MISSING**. `FeedbackReviewForm` (the staff review action's own DTO) has exactly two fields, `status` and `response` — no priority field at all, and `review()` never touches `feedback.setPriority(...)`. Priority can only ever be set once, by the member, at submission (`FeedbackForm.priority`); staff have no way to change it afterward.
- Administrator may close it without a response if not actionable — **BUILT**. `review()` allows `newResponse` to stay `null` while changing `status` to `Closed` — no response is required to close.

### Withdraw ("Delete", never a hard delete)
- **BUILT**. `withdraw()` sets `Status = Closed` (same eligibility gate as edit) and writes its own `FeedbackHistory` row, attributing the change to the member's own `AppUser` (`feedback.getMember().getUser()`, since `withdraw` has no separate staff actor).

### The one place a `FeedbackHistory` row is deliberately *not* written
`submit()` never writes a `FeedbackHistory` row for the initial `Submitted` state — documented at length in `FeedbackService`'s own class javadoc: `CK_FeedbackHistory_SomethingChanged` requires either a status change or a response change from some prior state, and a brand-new submission has no real "previous" state to diff against (`PreviousStatus` is `NOT NULL`, so there is no "not yet submitted" value to put there). This is the same category of schema-forced gap as UC-09's edit-history exception above — flagged in the code, not silently skipped, and not fixable without altering the schema.

### AuditLog check
`submit` (CREATE), `edit` (UPDATE), `withdraw` (WITHDRAW), `review` (UPDATE) are all `@AuditAction`-annotated. Fully covered on the audit-log side; the gap is specifically the member-facing Notification (above).

---

# Ranked gap list 1 — what a marker would notice in a live demo, worst first

1. **UC-08 does not exist at all.** Clicking "Reports" in the sidebar does nothing (`href="#"`). Any demo script that walks all ten use cases stops dead here — there is no screen, no data, nothing to click through. `com.lms.report` (`ReportController`, `ReportService`, `templates/report/`) needs to be built from scratch.
2. **UC-07's member-facing "Pay Fine" does not exist.** Log in as a member with an outstanding fine and there is no way to pay it — `fine/detail.html` has an Appeal button and nothing else. A marker asking "show me a member paying their own fine" gets nothing; only a staff account can record a payment on `/fines/{id}/pay`.
3. **UC-01's entire administrator-side user management screen does not exist.** No user list, no create/edit user, no role assignment, no deactivate button anywhere. `AppUserStatus.Deactivated` is checked at login but nothing in the app can ever set it.
4. **UC-10 never notifies a member their feedback got a response.** A very cheap, very visible check — submit feedback, have staff respond, look at the member's notifications (even the decorative bell) — and nothing appears, despite this being UC-10's own literal main-flow wording. `FeedbackService.review()` needs the same `notify()`-style addition `FineService` already has.
5. **UC-02's "Add New Book" form has no copy-count field.** A newly added book shows 0 of 0 copies available until a librarian separately visits "Add copy," once per physical copy — not the single flow the scenario (and a marker's expectation) describes.
6. **UC-05's staff Pay screen shows no days-overdue breakdown**, only the balance — a Finance Officer asked to "verify the fine amount" on camera has nothing on screen to verify it against.
7. **UC-09 gives an Administrator no direct "Remove" button on an already-Approved review.** They can only act after a member flags it first — a demo of "the administrator removes an inappropriate review" as its own action, unprompted by a flag, cannot be shown.
8. **UC-04's expired holds require a librarian to notice and click "Expire hold" by hand** — nothing detects an overdue hold automatically, so a demo of "the system cancels an uncollected reservation" needs manual intervention dressed up as automation.

# Ranked gap list 2 — wrong in the code but invisible in an ordinary demo

1. **`BorrowingService.issue()` has no `@AuditAction`.** The single most consequential write in the borrowing package — a new loan, a copy taken out of circulation — leaves no row in `AuditLog`, while `returnBook()` and both renewal paths do. Nothing on screen shows this is missing; it only surfaces if someone queries `AuditLog` after an issue and finds nothing.
2. **`com.lms.catalogue` has zero `@AuditAction` usages anywhere.** Every book/copy/author/category/publisher change (create, update, deactivate) is silent in the audit trail — the only feature package in the codebase with none at all.
3. **`ReservationService.fulfill()` and `.expire()` write no `Notification` and no `AuditLog` row.** `markReady` and `cancelByStaff` both do; these two siblings, doing an equally state-changing thing, do neither. A member is never actually told their reservation was fulfilled or that their hold expired.
4. **UC-05's own "Finance Officer refers a disputed calculation before accepting payment" is distinct from, and not satisfied by, UC-06's now-built appeal referral.** The two read almost identically in the scenario text; only the appeal-scoped one exists. Easy to mistake one for the other in review, since both produce a "Referred for correction" state somewhere.
5. **Five `NotificationType` enum values are declared and never used by any class**: `DueSoon`, `Overdue`, `RenewalDecision`, `ReviewDecision`, `FeedbackResponse` (the last confirmed as a genuine scenario gap above, not just dead code — see UC-10). No `@Scheduled` job exists anywhere in the codebase, so `DueSoon`/`Overdue` reminders were never reachable in the first place, by any path.
6. **UC-09's `edit()` and UC-10's `submit()` both silently skip a history row** (`ReviewModerationHistory` and `FeedbackHistory` respectively) for one specific transition each, because the schema's own CHECK constraints make that particular row impossible to write without a schema change. Both are documented in the code's own comments, but neither is visible from the UI — a demo of "every status change gets a history row" would need exactly the wrong two transitions clicked to expose either gap.
7. **UC-06's appeal decisions are approvable/rejectable by Finance Officer or Librarian, not just Library Administrator** as the scenario names — broader access than described, invisible unless someone reads the `@PreAuthorize` or specifically tests it with a non-Administrator staff account.
