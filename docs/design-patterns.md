# Design Patterns in This Codebase

## Part 1 — Patterns already present

### Repository
Spring Data JPA interfaces (`LoanRepository`, `FineRepository`, `ReservationRepository`, …
in `com.lms.common.domain`) hide SQL Server behind typed method names and
`@Query` methods. Every service depends on the interface, never on a
`JdbcTemplate` or an `EntityManager` directly.

### Dependency Injection
Every `@Service`/`@Controller`/`@Component` takes its collaborators as
constructor parameters — no field injection, no `new SomeRepository()`
anywhere in the codebase. Spring wires the graph; classes stay unit-testable
in isolation.

### MVC
`@Controller` classes (`LoanController`, `FineController`, …) hold no
business logic — they translate HTTP in, call one service method, translate
the result to a Thymeleaf view name or a redirect. Business rules live in
`@Service` classes (CLAUDE.md rule 5). Thymeleaf templates are the View.

### DTO
Every `com.lms.<feature>.dto` package (`FineListRow`, `ReturnResult`,
`AppealQueueRow`, …) is a `record` carrying exactly what one screen needs.
Entities never reach a controller or a template directly — services map
them inside their own transaction, which is also what stops a
`LazyInitializationException` after the Hibernate session closes.

### Template Method
`layout/base.html :: shell(title, content)` is the fixed algorithm — head,
theme script, sidebar, topbar, toast mount — with two open slots. Every
page (`home.html`, `fine/outstanding.html`, …) supplies just those two
fragments and `th:replace`s itself into the shell; it never writes `<html>`
or the sidebar itself.

### Aspect (AOP)
`@AuditAction(action = "...", entity = "...")` is a marker annotation;
`AuditAspect` (`@Aspect`, `@AfterReturning(pointcut = "@annotation(auditAction)")`)
writes the `AuditLog` row for every method that carries it, in one place,
without any service method calling an audit method itself.

### Facade
Each `@Service` (`FineService`, `ReservationService`, …) is a facade in
front of several repositories, business rules and cross-entity
calculations. A controller calls one method (`fineService.pay(...)`) and
never sees the repositories, entities or validation steps behind it.

### Chain of Responsibility
`SecurityConfig`'s `SecurityFilterChain` is Spring Security's own filter
chain — `CsrfFilter`, `SecurityContextHolderFilter`, `AuthorizationFilter`
and the rest each decide whether to act and then pass the request to the
next filter. This project only configures it (`http.authorizeHttpRequests(...)`),
but the pattern is genuinely doing the work on every request.

---

## Part 2 — Patterns implemented this pass

### 1. Strategy — fine calculation

**What it is.** One interface, one implementation per algorithm variant,
selected at runtime instead of branched on with `if`/`switch`.

**The problem here.** Overdue, Lost and Damaged fines are charged by three
unrelated formulas (business-rules.md §2/§5): `rate × days`, capped;
`purchase price + processing fee`; and `min(librarian's figure, purchase
price)`. The only one actually implemented lived **inline inside
`BorrowingService.returnBook()`** — the borrowing feature, not the fine
feature — because that is the one moment in this app a fine is raised
today. `FineService` itself never computed a charge; it only ever read
`AmountAssessed` back off an already-saved `Fine`.

**Before** (`BorrowingService.returnBook`, deleted by this pass):
```java
BigDecimal rate = fineRatePerDay();
BigDecimal cap = fineMaxPerLoan();
fineAmount = rate.multiply(BigDecimal.valueOf(daysOverdue)).min(cap).setScale(2, RoundingMode.HALF_UP);

Fine fine = new Fine();
fine.setMember(loan.getMember());
fine.setLoan(loan);
fine.setFineType(FineType.Overdue);
fine.setRatePerDay(rate.setScale(2, RoundingMode.HALF_UP));
fine.setAmountAssessed(fineAmount);
fines.save(fine);
```
A future Lost or Damaged charge would have meant pasting a second, third
near-identical block somewhere else, each hand-rolling its own
`SystemSetting` lookups.

**After.** `com.lms.fine.calculation.FineCalculationStrategy` is the
interface; `OverdueFineCalculationStrategy`, `LostFineCalculationStrategy`
and `DamagedFineCalculationStrategy` are its three implementations, each a
`@Component` Spring collects automatically. `FineService` selects one by
`FineType`:
```java
public FineChargeResult calculateCharge(FineType fineType, FineChargeRequest request) {
    FineCalculationStrategy strategy = chargeStrategies.get(fineType);
    if (strategy == null) {
        throw new IllegalStateException("No fine calculation strategy registered for " + fineType);
    }
    return strategy.calculate(request);
}
```
`chargeStrategies` is built once, in the constructor, from
`List<FineCalculationStrategy>` — Spring hands in every bean implementing
the interface, keyed by `FineCalculationStrategy::fineType`. The charge
calculation now lives in the fine feature, where the interface and
`FineType` already belong, and adding a fourth fine type later is one new
`@Component`, not a fourth branch somewhere in borrowing.

**Files:** `com.lms.fine.calculation.{FineCalculationStrategy,
FineChargeRequest, FineChargeResult, OverdueFineCalculationStrategy,
LostFineCalculationStrategy, DamagedFineCalculationStrategy}`,
`com.lms.fine.FineService`.

---

### 2. Factory — notifications

**What it is.** One class owns the recipe for building each kind of
object, so callers ask for what they mean ("an appeal was approved")
instead of assembling the object's fields themselves.

**The problem here.** Three services — `FineService`, `ReservationService`,
`MemberAdminService` — wrote `Notification` rows directly, eight call sites
total, each repeating the same five lines (`new Notification()`, four
setters, `notifications.save(...)`) with only the type/title/message text
differing. `FineService` and `MemberAdminService` had each already grown
their own private `notify(...)` helper to cut the repetition inside their
own class; `ReservationService` had not, so its two call sites were fully
inline. Three classes, three (or zero) private conventions, no single
place that says what an "appeal approved" notification actually says.

**Before** (`ReservationService.markReady`, one of the two inline sites):
```java
Notification notification = new Notification();
notification.setUser(reservation.getMember().getUser());
notification.setNotificationType(NotificationType.ReservationReady);
notification.setTitle("Reservation ready for collection");
notification.setMessage("\"" + book.getTitle() + "\" is being held for you until "
        + expiresAt.toLocalDate() + ". Collect it at the front desk.");
notifications.save(notification);
```

**After.** `com.lms.common.domain.NotificationFactory` is a `@Component`
with one method per notification kind — `appealApproved`, `appealRejected`,
`reservationReady`, `reservationCancelledByStaff`, `membershipApproved`,
`membershipRejected`, `membershipSuspended`, `membershipReactivated` — each
building the `Notification` and handing it back unsaved:
```java
public Notification reservationReady(AppUser recipient, String bookTitle, LocalDateTime expiresAt) {
    return build(recipient, NotificationType.ReservationReady, "Reservation ready for collection",
            "\"" + bookTitle + "\" is being held for you until " + expiresAt.toLocalDate() + ". Collect it at the front desk.");
}
```
Every call site now reads the same way, in all three services:
```java
notifications.save(notificationFactory.reservationReady(reservation.getMember().getUser(), book.getTitle(), expiresAt));
```
The three private `notify(...)` helpers (two of which duplicated each
other almost exactly) are gone; the wording for a given notification kind
now exists exactly once.

**Files:** `com.lms.common.domain.NotificationFactory`; call sites in
`com.lms.fine.FineService` (`approveAppeal`, `rejectAppeal`),
`com.lms.reservation.ReservationService` (`markReady`, `cancelByStaff`),
`com.lms.user.MemberAdminService` (`approve`, `reject`, `suspend`,
`reactivate`).

---

### 3. Observer — `LoanReturnedEvent`

**What it is.** The object where something happens publishes an event and
moves on; objects that care about it subscribe independently. The
publisher does not know or call its listeners.

**The problem here.** `BorrowingService.returnBook()` did the loan-closing
work (mark returned, free or damage the copy) and then, inline, also
calculated and saved the Overdue fine — a fine-domain concern sitting
inside the borrowing service, coupling the two features together for no
reason beyond "this is the one place a return happens."

**What this pass deliberately did not add:** reservation queue promotion.
`ReservationService.markReady` has always been a manual staff action — a
librarian's own decision from the Reservation Queue screen, documented as
a deliberate scope limit in that class's own javadoc — and `returnBook()`
never touched a reservation at all. Publishing an event that *automatically*
promoted the next reservation would be a new, user-visible behaviour this
app has never had, not a refactor of existing behaviour, so it was not
built. The reservation listener below observes the event and reports, on
the server log, that a promotion is now *possible* — the same fact the
Reservation Queue screen's own `canMarkReady` flag already derives — and
leaves the actual decision exactly where it already lived.

**Before** (`BorrowingService.returnBook`, abridged):
```java
copies.save(copy);

BigDecimal fineAmount = fines.findByLoanLoanIdAndFineType(loan.getLoanId(), FineType.Overdue)
        .map(Fine::getAmountAssessed).orElse(null);
if (fineAmount == null) {
    long daysOverdue = ChronoUnit.DAYS.between(loan.getDueAt(), now);
    if (daysOverdue > 0) {
        // ~10 lines building and saving a Fine, here, in the borrowing service
    }
}
```

**After.** `returnBook()` publishes one event and reads the outcome back
for the UI response — it no longer knows fines exist:
```java
events.publishEvent(new LoanReturnedEvent(loan.getLoanId(), loan.getMember().getMemberId(),
        copy.getBook().getBookId(), copy.getCopyId(), loan.getDueAt(), now, damaged));

BigDecimal fineAmount = fines.findByLoanLoanIdAndFineType(loan.getLoanId(), FineType.Overdue)
        .map(Fine::getAmountAssessed).orElse(null);
```
`FineService.onLoanReturned` (`@EventListener`) does the calculation, now
through the Strategy from Part 2:
```java
@EventListener
public void onLoanReturned(LoanReturnedEvent event) {
    if (fines.findByLoanLoanIdAndFineType(event.loanId(), FineType.Overdue).isPresent()) {
        return;
    }
    Loan loan = loans.getReferenceById(event.loanId());
    FineChargeResult charge = calculateCharge(FineType.Overdue, FineChargeRequest.forOverdueReturn(loan, event.returnedAt()));
    if (charge.amount().compareTo(BigDecimal.ZERO) <= 0) {
        return;
    }
    Fine fine = new Fine();
    fine.setMember(loan.getMember());
    fine.setLoan(loan);
    fine.setFineType(FineType.Overdue);
    fine.setRatePerDay(charge.ratePerDay());
    fine.setAmountAssessed(charge.amount());
    fines.save(fine);
}
```
`ReservationService.onLoanReturned` is the second, independent listener —
read-only, as decided above:
```java
@EventListener
public void onLoanReturned(LoanReturnedEvent event) {
    if (event.damaged()) {
        return;
    }
    reservations.findFirstByBookBookIdAndStatusOrderByRequestedAtAsc(event.bookId(), ReservationStatus.Waiting)
            .ifPresent(next -> log.info("Copy {} of book {} is available again; {} is next in the reservation queue "
                    + "(waiting since {}) and can now be marked Ready.",
                    event.copyId(), event.bookId(), next.getMember().getMembershipNo(), next.getRequestedAt()));
}
```
Both listeners are plain `@EventListener`, not `@TransactionalEventListener`
— Spring invokes them synchronously, in the publisher's own call stack, so
they run inside `returnBook()`'s existing `@Transactional` boundary exactly
as the inline code did: a failure in fine creation still rolls back the
whole return.

**Files:** `com.lms.common.event.LoanReturnedEvent`;
`com.lms.borrowing.BorrowingService` (`returnBook`, publisher);
`com.lms.fine.FineService` (`onLoanReturned`, listener);
`com.lms.reservation.ReservationService` (`onLoanReturned`, listener).
