# Self-Assessment Against the IT2140 Part 02 Marking Schema
Group MLB-B11G2-10 · Graded against `docs/it2140-marking-schema.pdf` and
`docs/it2140-part02-assignment.pdf`. Evidence taken only from what is
actually committed under `database/` (and, for Part A, `docs/er-diagram.png`)
and from queries actually executed against the live SQL Server instance this
session, with output pasted below where it changes the mark. No credit given
for work done live in the running web application that was never turned into
a file in this submission.

This supersedes the previous version of this file. The main thing that
changed since then is `database/09_demo_data.sql`, a new, uncommented,
insert-only script that adds rows to sixteen tables that previously had
zero. It materially improves Part C and partially, but not fully, closes a
defect flagged against Part D — the reasoning below explains exactly why
"partially."

---

## Part A — EER → Relational Schema Mapping (10 marks)

**Band: Excellent (9–10).** *"All entities, PKs, FKs, constraints correctly
mapped. ISA handled precisely with clear justification."*

**Mark: 9/10.**

`docs/er-diagram.png` is a real, dense EER — five swim-laned regions,
explicit ISA triangles for `APP_USER → {STAFF_PROFILE, MEMBER}` and
`MEMBER → ACADEMIC_STAFF`, M:N diamonds with their own attributes
(`ASSIGNS ROLE`, `WRITES`, `CLASSIFIES`), and a reified aggregation box
around `BORROWS`/`LOAN`. Checked directly against it, `00_relational_mapping.md`
maps every one of those boxes, plus `BOOK_KEYWORD` for the multivalued
`[M] Keywords` attribute drawn inside `BOOK`, plus one table
(`PasswordResetToken`) it explicitly discloses is *not on the diagram at
all*. Spot-checking the refinement table's "dropped" column against the
image confirms every one of those attributes is genuinely drawn on the
diagram and genuinely absent from `01_schema.sql`: `MEMBER.BorrowingLimit`,
`ACADEMIC_STAFF.CanReserveUnavailableBooks`, `RESERVATION.QueuePosition` /
`Priority`, `LOAN.RenewalCount`, `BOOK_INCIDENT.ReplacementCost` /
`RepairCharge`, `FINE.DaysOverdue` / `WaivedAmount` — all visibly present as
attributes in the image, all absent from the DDL. Both ISA strategy choices
are argued against the *rejected* alternatives with a named anomaly each,
not asserted by fiat.

**What is missing or weak, in priority order:**
1. No explicit entity-by-entity traceability table. The diagram has roughly
   29 named entity/subtype boxes (counted directly off the image); the
   mapping document states "Relation count: 32" in prose and expects the
   reader to reconcile the two by working through six pages of running
   text. A marker "looking for reasons to deduct" reads an unverified claim,
   not a verified one, even though it happens to be accurate.
2. Refinement **#30** (ISA changed from disjoint to overlapping — the
   single highest-stakes decision in the document, since it changes a
   Part 01 answer) is defended entirely in one wide table cell of prose. It
   is easy to skim past in a viva.
3. The document does not separately flag which diagram attributes were
   *reinterpreted* rather than kept or dropped outright — e.g.
   `FINE.RatePerDay` / `AmountAssessed` are kept as policy **snapshots**,
   which is a different claim from "kept as-is," but the document does not
   name this as its own category the way "dropped" (§4) and "added" (#28)
   are.

**Easy fix:** add one table at the top of §3 — `Entity | Relation(s) it
became | Row in §4 if changed` — roughly 29 rows, a five-minute
transcription of facts already established elsewhere in the same document.
That turns "a marker can verify this by reading everything" into "a marker
does not have to."

---

## Part B — SQL DDL Implementation (20 marks)

**Band: Good (15–17).** *"Minor syntax errors, most constraints implemented
correctly."* The DDL substance is stronger than this band's own wording
implies — there are no syntax errors and every constraint, not merely most,
is implemented — but Excellent's own qualifier, *"consistent schema,"* is
demonstrably false of this submission for a specific, checkable reason
below, which is disqualifying on the marking schema's own terms rather than
a matter of degree.

**Mark: 17/20.**

`01_schema.sql` (1064 lines) is genuinely comprehensive: every table has a
named PK, every FK is typed with an explicit, justified `ON DELETE` policy
(`CASCADE` only for pure dependants — `UserRole`, `BookAuthor`,
`BookCategory`, `BookKeyword`, `FeedbackHistory`), and every domain column
carries a `CHECK` naming its legal values. Eleven of those checks are the
*composite* kind most student DDL never attempts —
`CK_Loan_ReturnFieldsTogether`, `CK_Fine_SourceMatchesType`,
`CK_ReviewModerationHistory_ModeratorRequired`, `CK_Fine_WaiverDetails` —
each enforcing a multi-column business rule, not just a domain. Four rules
that genuinely cannot be single-row `CHECK`s (max-2-renewals,
outstanding-balance arithmetic, the fine-status lifecycle, the
copy-status lifecycle) are correctly identified as such in the mapping
document and pushed to Part F rather than faked with an unenforceable
comment.

**What is missing or weak, in priority order:**
1. **`database/04_security_migration.sql` duplicates a table
   `01_schema.sql` already defines.** `PasswordResetToken` and the four
   `Security.*` `SystemSetting` rows are created natively inside
   `01_schema.sql` (confirmed by reading both files directly — the table at
   lines 307–333, the four settings at lines 1059–1062), yet
   `04_security_migration.sql` still exists as a second,
   `IF OBJECT_ID(...) IS NULL`-guarded path to create the exact same
   objects, and its own `SystemSetting` insert is a guaranteed no-op
   against a database built from `01_schema.sql` (`WHERE NOT EXISTS` on
   keys that already exist). CLAUDE.md rule 1 states, in the project's own
   words, *"The schema is defined ONLY in `database/01_schema.sql`"* —
   right now that sentence is false of this repository. This is exactly
   the phrase Excellent's own descriptor uses ("consistent schema"), so a
   marker applying the rubric literally has a concrete reason to withhold
   the top band, independent of how correct either file is in isolation.
2. **No supporting index on any hot, non-filtered foreign key.** Grepping
   `01_schema.sql` for every `CREATE ... INDEX` shows nine indexes total,
   and all nine exist to enforce a business rule (`UX_Loan_OneOpenLoanPerCopy`,
   `UX_Reservation_OneActivePerMemberBook`, `UX_Fine_OneOverduePerLoan`,
   `IX_Reservation_Queue`, …), not to speed up a join. There is no index at
   all on `Loan.MemberID`, `Loan.CopyID` (unfiltered), `Fine.MemberID`,
   `Reservation.BookID`, `Reservation.MemberID`, or `BookCopy.BookID` — the
   exact columns `05_queries.sql` joins through repeatedly (Q3 alone joins
   `Loan.MemberID`, `Loan.CopyID` and `BookCopy.BookID` in one statement).
   LO4 explicitly names "efficient server-side data management"; every one
   of those joins is currently a scan on the child side, on a schema that
   already has 43 `BookCopy` rows, 36 `Loan` rows and would only grow.
3. Minor: `03_demo_passwords.sql` and `04_security_migration.sql` are
   numbered as if part of the same pipeline as `01`–`02_seed_03`, and the
   new `09_demo_data.sql` continues that numbering, but none of the three
   is named in the assignment's own six-part structure, so a marker working
   strictly from the brief may not know whether — or when — to run them.

**Easy fix:** delete `04_security_migration.sql` (everything it does is
already in `01_schema.sql`, confirmed above) and add five
`CREATE NONCLUSTERED INDEX` statements for the FK columns named in point 2.
Fifteen minutes, and it removes the one concrete, quotable sentence keeping
this out of Excellent.

---

## Part C — Insert Sample Data (10 marks)

**Band: Satisfactory (5–6).** *"Data inserted but lacks completeness or
variety."* That is a precise description of the current state: real,
varied, constraint-valid data now exists for almost every functional area
of the system, but a third of the tables still fall short of the
assignment's own explicit "at least 5 records per table," and one table
has none at all.

**Mark: 6/10.**

Counting every `INSERT` across every file that adds rows to a business
table — `02_seed_01_identity.sql`, `02_seed_02_catalogue.sql`,
`02_seed_03_demo.sql`, `01_schema.sql`'s own `SystemSetting` seed, and the
new `09_demo_data.sql` — against all 33 tables in `01_schema.sql`
(32 EER-mapped relations plus `PasswordResetToken`, which the mapping
document itself discloses as an addition):

| Meets "≥5 records" (23 tables) | Below 5, non-zero (9 tables) | Zero rows (1 table) |
|---|---|---|
| Role(6), AppUser(6), UserRole(6), StaffProfile(6), Member(6), Publisher(6), Author(15), Category(8), Book(12), BookAuthor(16), BookCategory(17), BookKeyword(50), BookCopy(42), SystemSetting(13), FeedbackCategory(5), **Loan(26)**, **Fine(10)**, **BookReview(10)**, **MemberFeedback(7)**, **FeedbackHistory(15)**, **Notification(7)**, **AuditLog(20)**, **FailedLoginAttempt(6)** | LoanRenewal(2), Reservation(4), BookIncident(1), FineAppeal(1), FinePayment(2), ReviewFlag(1), ReviewModerationHistory(2), ReportAudit(4), DatabaseBackupLog(2) | PasswordResetToken(0) |

(Row counts for `BookAuthor`/`BookCategory`/`BookKeyword` were recounted
directly from `02_seed_02_catalogue.sql`'s own `VALUES` lists rather than
trusted from that file's header comments, because the header comments are
wrong — see the Part B–adjacent note below. Row counts for every table
`09_demo_data.sql` touches were reconciled against `SELECT COUNT(*)`
against the live database immediately before and immediately after running
that one file, isolating its contribution from unrelated rows the running
application had already written this session.)

Bold entries are the eleven tables that moved from **zero** rows (the
previous version of this assessment listed sixteen tables at zero) into the
"meets the instruction" column purely because of `09_demo_data.sql`. That
is a real, substantial improvement: `BookReview`, `MemberFeedback`,
`FeedbackHistory`, `AuditLog`, `Notification` and `FailedLoginAttempt` all
went from having *no* demonstrated sample data anywhere in the submission
to having a genuine, FK-consistent, constraint-respecting set of rows
covering realistic scenarios (an overdue loan, a partially-paid fine with
its receipt, a lost-book incident and its fine and appeal, a waived fine
with its reason and staff, a flagged-and-hidden review, a full
Submitted→Resolved feedback history trail). Every row in every file was
verified to insert cleanly against the live schema (`sqlcmd -I` run to
completion, zero errors, all `CK_*` constraints — including the composite
ones — satisfied by construction, not by luck).

The instruction is still not met for nine tables, and `PasswordResetToken`
has never once been populated by any script in this repository (its one
live row exists only because a real user went through the password-reset
flow in the running application, which this assessment does not credit,
consistent with how it treats every other piece of live-only data).

The Part C instructions also require *"Provide screenshots of inserted data
(SELECT *)."* **There are zero screenshots anywhere in this repository** —
`docs/` contains exactly one image, the ER diagram, and no seed file
references or links to any image. This is unchanged from before. Read
strictly, the marking schema's own bottom band is defined by *"No valid
data inserted **or** screenshots missing"* — screenshots are not thin here,
they are entirely absent, which is the literal trigger for 0–2. The mark is
set in Satisfactory rather than Very Poor for the same reason the previous
version of this document gave for landing above the literal floor: the
Satisfactory band's own descriptor is the only one of the five that does
not mention screenshots at all, and the data that exists now clearly
exceeds "data inserted but lacks completeness or variety" without
qualifying for Good's implicit claim that *some* screenshot evidence
exists.

**What is missing or weak, in priority order:**
1. **Zero screenshots, still.** This is the single largest, easiest-to-fix
   gap against the assignment's own explicit instruction, and it caps this
   part below Good regardless of how complete the data becomes.
2. Nine tables remain below the stated minimum of 5, most seriously
   `Reservation` (4), `FineAppeal` (1), `FinePayment` (2), `BookIncident`
   (1) and `ReviewFlag`/`ReviewModerationHistory` (1–2 each) — every one of
   these backs a named use case (UC-04, UC-05/06/07, UC-09) and is now
   *represented* but not *complete* by the assignment's own rule.
3. `PasswordResetToken` has zero rows from any script.
4. Three of `02_seed_02_catalogue.sql`'s own header comments understate
   the row counts they actually insert — `BookAuthor` says "14 rows" and
   contains 16, `BookCategory` says "16 rows" and contains 17,
   `BookKeyword` says "48 rows" and contains 50. Harmless to correctness,
   but it is exactly the kind of small, checkable inconsistency a marker
   "looking for reasons to deduct" finds by counting instead of trusting a
   comment, and it undermines confidence in every other stated count in the
   file.

**Easy fix:** take `SELECT *` screenshots of the now-populated tables (the
data already exists and is already correct — this is photography, not
engineering, maybe 30–45 minutes for the highest-value tables) and top up
the nine still-short tables with two or three more rows each. `Reservation`
and `FinePayment` are the cheapest wins, since both already have a working
example row in `09_demo_data.sql` to copy the shape of.

---

## Part D — SQL Queries & Outputs (20 marks)

**Band: Poor (6–10).** *"Major query errors, missing screenshots or
explanations."* No query in `05_queries.sql` is logically wrong, but the
"screenshots or explanations" clause is unqualifiedly true, and a specific,
reproducible sequencing defect from the previous version of this assessment
is **not actually fixed** by the new sample-data file, for a reason
explained below that a harsh marker would catch immediately.

**Mark: 10/20.** Unchanged from the previous assessment.

`05_queries.sql` covers real breadth: a plain `SELECT`, a two-table and a
five-table `INNER JOIN`, a `LEFT JOIN` with a `NULL` check, an ISA-hierarchy
join (Q5), `GROUP BY`/`HAVING`, a plain and a correlated subquery, a derived
table, `EXISTS`/`NOT EXISTS`, `IN`/`NOT IN`, `UNION`, a window function
(`ROW_NUMBER() OVER (PARTITION BY ...)`), `CASE`, date arithmetic two ways,
a payments-to-balance computation, and `LIKE`. Running the whole file
against the live database right now, every one of the twenty queries
returns rows and none errors:

```
-- Q8 (correlated subquery over BookReview), run live just now:
ReviewID|BookID|Rating|ReviewText
3|2|5|good knowlage
6|3|4|Solid coverage of the software development lifecycle.

-- Q9 (derived table over FinePayment):
MemberID|MemberName|TotalPaid
1|Nuwan Perera|800.00
8|user1 LB|160.00

-- Q10 (EXISTS over Reservation):
MemberID|MemberName
2|Ama Silva
3|Kasun Fernando
5|Dilani Rathnayake
6|Sanduni Jayawardena
8|user1 LB
9|user2 LB

-- Q15 (window function over Reservation):
ReservationID|Title|MemberName|RequestedAt|QueuePosition
8|Clean Code|user1 LB|...|1
9|Clean Code|Ama Silva|...|2
```

That looks like the previous assessment's flagged defect (Q8, Q9, Q10, Q15
returning nothing on a fresh seed) is now resolved. **It is not**, and this
is the sharpest finding in this document: `02_seed_03_demo.sql` — the file
that runs immediately before `05_queries.sql` in the numbered sequence —
inserts zero rows into `BookReview`, `Reservation` or `FinePayment`, exactly
as before. The rows that make Q8/Q9/Q10/Q15 non-empty above all come from
`09_demo_data.sql`, which is numbered **after** `05_queries.sql`. A marker
who runs this repository's files in the order its own filenames say to run
them — `01` → `02_seed_01` → `02_seed_02` → `02_seed_03` → `05_queries.sql`
— hits the exact same four empty result sets the previous version of this
assessment reported, because the file that would fix it has not run yet at
that point. The new sample data genuinely fixes the underlying problem only
for a marker generous enough to run every file in the repository first and
test queries last, which is not what the numbering promises. This is why
Part D's mark has not moved even though the data gap that caused it
demonstrably has been narrowed elsewhere in the submission.

**What is missing or weak, in priority order:**
1. **The fix for the four empty queries is misfiled.** `09_demo_data.sql`
   is numbered `09`; `05_queries.sql` is numbered `05`. Renumbering the new
   file to run before the queries (or duplicating its `BookReview` /
   `Reservation` / `FinePayment` rows earlier, into `02_seed_03_demo.sql`)
   is what actually closes this gap under a literal, in-order run —
   nothing about the SQL in `05_queries.sql` itself needs to change.
2. **Zero screenshots**, for any of the twenty queries — explicitly
   required by the Part D instructions.
3. **No explanation beyond the one-line question comment above each
   query.** The brief asks for "screenshots of query + output **with
   explanations**"; a comment stating the business question a query
   answers is not an explanation of the SQL technique used to answer it
   (why a correlated subquery rather than a window function for Q8, why
   `NOT EXISTS` rather than `LEFT JOIN ... IS NULL` for Q11, and so on).

**Easy fix:** renumber `09_demo_data.sql` to run before `05_queries.sql` (or
copy just its `BookReview`/`Reservation`/`FinePayment`/`FineAppeal` inserts
into `02_seed_03_demo.sql`) — this is a five-minute rename/move that turns
the sequencing defect into a genuinely closed gap, on data that already
exists and is already correct.

---

## Part E — Stored Function/Procedure (15 marks)

**Band: Excellent, low end (13–15).** *"Fully functional and relevant
procedure/function with demonstration and correct output."*

**Mark: 13/15.** Unchanged — `06_procedure.sql` was not touched since the
previous assessment, and re-reading it in full confirms the same facts.

`sp_IssueBook` is a real, non-trivial procedure: it enforces all five named
business rules (membership exists/Active/not expired, borrowing limit from
`SystemSetting`, no overdue loan, no unpaid overdue fine, copy
exists/Available/not reference-only) as nine distinctly numbered `THROW`s
(50001–50009, plus 50020/50021 for missing configuration), wrapped in an
explicit transaction with `TRY`/`CATCH` and a `ROLLBACK` guarded by
`XACT_STATE()`. `UPDLOCK, HOLDLOCK` on the `Member` and `BookCopy` reads is
a genuine concurrency-correctness detail — reasoned about, not accidental —
matching LO4's own "strong understanding of execution plans" framing far
more than a plain `SELECT` would. Ten numbered test cases (T1–T10) at the
bottom exercise every `THROW` path, including two that require temporarily
mutating seeded state (`T3` suspends then restores a membership, `T5` zeros
then restores a borrowing-limit setting) and clean up after themselves.

**What is missing or weak, in priority order:**
1. **No output screenshots exist anywhere**, explicitly requested by Part
   E's own instructions ("Execute with sample input... Provide code +
   output screenshots").
2. The test block's own header says it must run "against a freshly seeded
   database," but nothing in this repository demonstrates it was actually
   executed against a genuinely fresh `01_schema.sql` + seed-script run —
   the live database it was most recently proven against had already
   accumulated nine months of session testing and demo data before this
   procedure's test block was last run, which is a materially different
   starting state from what the file's own comment claims.

**Easy fix:** restore a database from `01_schema.sql` + the three
`02_seed_*` files only, run `06_procedure.sql` end to end in one pass, and
screenshot the ten test outputs. Twenty minutes, no code changes, and it
simultaneously produces the missing evidence and proves the file's own
documented assumption true.

---

## Part F — Trigger (15 marks)

**Band: Excellent, low end (13–15).** *"Fully functional trigger with
correct logic and successful demonstration."*

**Mark: 13/15.** Unchanged — `07_trigger.sql` was not touched since the
previous assessment.

`trg_Loan_AfterReturn` builds an `inserted`/`deleted` join into a table
variable rather than a scalar assignment — the exact discipline needed for
a genuine multi-row `UPDATE` to fire correctly for every affected row, not
silently just one — and its own packaged test (`T1`) proves this on a
real two-row `UPDATE`, checking that both copies return to `Available`,
that exactly one `Fine` row is written (only the overdue one), and that two
distinct `AuditLog` rows carry valid per-row JSON `Details`. `T2` proves the
`IF NOT UPDATE(ReturnedAt)` guard actually stops the trigger re-firing on an
unrelated column edit to an already-returned row, which is the specific
failure mode a `NULL`→value-transition trigger is most likely to get wrong.

**What is missing or weak, in priority order:**
1. **The trigger sets every returned copy to `Available` unconditionally,
   even when `ReturnCondition = 'Damaged'`.** `business-rules.md` §5 treats
   a damaged return as taking the copy out of circulation via
   `BookIncident`, not putting it straight back on the shelf. The Part F
   brief's own instructions say only "updates/validates/audits data" with
   no carve-out, so this is a literal implementation of the task as given —
   but a marker testing it by hand with a `Damaged` return will watch a
   damaged book become immediately borrowable again, which contradicts this
   project's own binding business rule, not a hypothetical one.
2. **No output screenshots exist anywhere**, again explicitly requested by
   Part F's instructions.
3. Same caveat as Part E: the packaged test block documents a
   fresh-database assumption that nothing in this repository proves was
   actually exercised against a genuinely fresh restore.

**Easy fix for #1:** `ReturnCondition` arrives in the same statement as
`ReturnedAt` (`CK_Loan_ReturnFieldsTogether` guarantees it), so it is
already available on `inserted`. Add it to the `@Returned` table variable
and change the `BookCopy` update to
`SET c.Status = CASE WHEN r.ReturnCondition = N'Damaged' THEN N'Damaged' ELSE N'Available' END`.
Ten minutes, and it converts a real logic gap into fully correct behaviour.

---

## Highest-value fix per part, ranked by marks gained per hour

1. **Part D — renumber or relocate `09_demo_data.sql`'s `BookReview` /
   `Reservation` / `FinePayment` rows to before `05_queries.sql`.**
   ~5 minutes for a likely 3–4 marks — the data already exists and is
   already correct; only its position in the file sequence is wrong.
2. **Part F — make the trigger respect `ReturnCondition = 'Damaged'`.**
   ~10 minutes for a likely 1–2 marks, and it fixes an actual bug against
   this project's own business rules, not just the grade.
3. **Part B — delete the redundant `04_security_migration.sql`, add five
   FK indexes.** ~15 minutes for a likely 1–2 marks. Purely mechanical.
4. **Part E — reseed fresh and capture real screenshots of
   `06_procedure.sql`'s existing (already-correct) test run.** ~20 minutes,
   no code changes, for a likely 1–2 marks.
5. **Part C — screenshot the now-populated tables and top up the nine
   still-short ones.** ~45–60 minutes (the hard engineering work — writing
   FK- and constraint-respecting sample data for `Reservation`,
   `FineAppeal`, `FinePayment`, `BookIncident` and the review/moderation
   chain — is now already done; what is left is screenshots plus two or
   three more rows per short table), for a likely 2–3 marks. Do this
   together with fix #1, since both draw on the exact same underlying data.
6. **Part A — add the ~29-row entity→relation traceability table.**
   ~5 minutes for a likely 1 mark; smallest absolute gain on this list, but
   trivial to execute.
