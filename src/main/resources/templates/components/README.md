# Shared components

Every screen uses these instead of hand-writing its own table, form field,
status badge, empty state, loading state, modal, toast, pagination bar or
page heading (CLAUDE.md rule 7: "Every UI screen uses the shared fragments
in `templates/components/`. Write no new CSS and introduce no new colours.")

All CSS for these lives in `static/css/app.css`. None of the fragments
below invent a class, a colour or a spacing value that file doesn't already
define. If a screen needs something these fragments genuinely can't do,
that's a conversation with whoever owns the shared foundation (Shashith),
not a reason to write page-local CSS.

Two small Java-side helpers back these fragments, both in
`com.lms.common.web`:

- `StatusPillMapper` — every status-to-colour decision, in one file.
- `DataTableColumn` / `SelectOption` / `StatTile` — small records so a
  controller builds a table's columns or a dashboard's tiles as typed Java,
  not string-keyed maps in a template.

## Three Thymeleaf gotchas these fragments already work around

Found the hard way while building `home.html` as the first real page to use
every component at once — worth knowing before you hit them again in a new
fragment:

1. **`th:each` and a *parameterised* fragment reference
   (`~{template :: name(args)}`) don't combine reliably on one tag** — the
   arguments can evaluate before that tag's own loop variable is bound.
   `data-table.html` avoids it by putting `th:each` on a `<th:block>`
   wrapper and `th:replace` on the plain child `<tr>` beneath it. A
   *parameterless* reference to a variable already holding the fragment
   (`th:replace="${rowFragment}"`) is fine directly on a `th:each` tag —
   that part was never the problem.
2. **`th:if`/`th:unless` and `th:insert`/`th:replace` on one tag don't
   combine reliably either** — the fragment can render regardless of the
   condition. `data-table.html`'s empty state and `modal.html`'s optional
   footer both use the same fix: the condition on an outer wrapper, the
   fragment call on a plain child inside it.
3. **A string literal passed as an argument inside a `~{template ::
   name(args)}` fragment expression can't contain an apostrophe at all —
   not even backslash-escaped.** Elsewhere, `'don\'t'` is a normal escaped
   OGNL string. Inside `~{...}}`'s own argument-list parser it isn't:
   attoparser fails the whole expression with a `ParseException`
   ("Could not parse as expression") whether the quote is escaped or not.
   Found building `user/register.html` and `error/access-denied.html`
   (both passed body text with an apostrophe straight to `page-header`/
   `empty-state`). The fix isn't an escaping trick — there isn't one that
   works here — it's rephrasing the string to not need an apostrophe
   ("librarian's approval" → "approval from a librarian", "don't" → "do
   not").

If a new fragment needs any of these, work around it the same way rather
than trying it again on one tag.

---

## status-pill

```html
<span th:replace="~{components/status-pill :: statusPill('loan', loan.status)}"></span>
```

Renders the pill's colour **and** its text from one value — you never pass
the label separately, so the two can't say different things.

`domain` is which status column `status` belongs to. There are fourteen —
one per status enumeration in `database/01_schema.sql` — and every value
each one can hold is mapped in `StatusPillMapper`. Two of them (`loan`,
`membership`) also accept a value that is *never stored* — `"Overdue"` and
`"Expired"` are computed from a date (see `01_schema.sql` R15 / R26), not a
column, so pass whatever String your service computed for those.

| `domain`      | column(s)                                                   | values |
|---------------|--------------------------------------------------------------|--------|
| `appUser`     | `AppUser.Status`                                              | Active, Deactivated, Locked |
| `membership`  | `Member.MembershipStatus`                                     | Active, Suspended, Cancelled, *Expired (derived)* |
| `bookCopy`    | `BookCopy.Status`                                              | Available, On Loan, On Hold, Under Repair, Damaged, Lost, Withdrawn |
| `loan`        | `Loan.Status`                                                  | Active, Returned, Lost, *Overdue (derived)* |
| `renewal`     | `LoanRenewal.Status`                                           | Pending, Approved, Rejected |
| `reservation` | `Reservation.Status`                                           | Waiting, Ready, Fulfilled, Cancelled, Expired |
| `incident`    | `BookIncident.Status`                                          | Open, Charged, Resolved, Written Off |
| `fine`        | `Fine.Status`                                                  | Pending, Under Appeal, Partially Paid, Fully Paid, Waived |
| `appeal`      | `FineAppeal.Status`                                            | Pending, Approved, Rejected |
| `payment`     | `FinePayment.PaymentStatus`                                    | Completed, Failed, Refunded |
| `review`      | `BookReview.Status`, `ReviewModerationHistory.*Status`         | Pending, Approved, Rejected, Hidden, Removed |
| `flag`        | `ReviewFlag.Status`                                            | Open, Upheld, Dismissed |
| `feedback`    | `MemberFeedback.Status`, `FeedbackHistory.*Status`              | Submitted, Under Review, In Progress, Resolved, Closed |
| `backup`      | `DatabaseBackupLog.Status`                                     | Running, Succeeded, Failed |

`status` can be the entity's field exactly as it comes off the entity — an
enum like `BookCopyStatus.OnLoan` or a `DbValueEnum` one, doesn't matter
which — or a plain derived `String`.

**Adding a fifteenth domain?** Add a `case` to the `switch` in
`StatusPillMapper.token()` (and a row to the table above) — don't guess a
colour by copy-pasting a `status-pill--*` class in a template. Read that
class's javadoc first: it explains what each of the six colours means here
(they're reused for more than their literal names — "on-loan" covers
anything currently in progress, "reserved" covers anything waiting on
someone else's decision) so a new mapping stays consistent with the
existing fifty-odd ones instead of picking whichever hue looks closest.

---

## page-header

```html
<div th:replace="~{components/page-header :: pageHeader('Books', 'Search and manage the catalogue.',
                                                          'Add Book', @{/catalogue/books/new}, 'plus')}"></div>
```

`description`, and the whole action button (`actionText` + `actionHref` +
optional `actionIcon`), are all optional — pass `null` for any of them.

---

## data-table

This is the one with a real trick to it, because a generic fragment cannot
know how to render *your* row — a loan row needs a status pill and a mono
due date, a fine row needs completely different cells. So you write the
`<tr>` yourself, as your own named fragment with **no parameters**, and
`data-table` calls it once per row, handing it `row` implicitly.

**1. Build the columns and fetch the rows in your controller:**

```java
@GetMapping("/catalogue/books")
public String list(@RequestParam(defaultValue = "1") int page,
                    @RequestParam(required = false) String sort,
                    @RequestParam(defaultValue = "asc") String dir,
                    Model model) {
    model.addAttribute("columns", List.of(
            DataTableColumn.left("Title", "title"),
            DataTableColumn.left("Author"),
            DataTableColumn.right("Copies", "copies")));
    model.addAttribute("rows", bookService.page(page, sort, dir));
    model.addAttribute("currentSort", sort);
    model.addAttribute("currentDir", dir);
    return "catalogue/books";
}
```

**2. Define the row fragment in a companion `*-fragments.html` file** next
to the page template — note: no `(row)` in the `th:fragment` name:

```html
<!-- templates/catalogue/books-fragments.html -->
<html xmlns:th="http://www.thymeleaf.org">
<body>
<tr th:fragment="bookRow">
    <td><span class="book-title" th:text="${row.title}">Title</span></td>
    <td th:text="${row.authorName}">Author</td>
    <td class="data-table__col-right font-mono" th:text="${row.copyCount}">3</td>
</tr>
</body>
</html>
```

**Why a separate file, not just inside `books.html` itself:** `books.html`
is a real page — `BookController` returns it directly, so Thymeleaf renders
it top to bottom on every request. Declaring a fragment doesn't hide it
from that normal rendering; a `<tr th:fragment="bookRow">` sitting inside
`books.html` would ALSO render once at its own position in the page, where
`row` is undefined (it only exists inside `data-table`'s own `th:each`) —
a hard error, not a formatting glitch. A file no controller ever returns as
a view name — like this one, like everything already in `components/` —
is never rendered on its own, only ever reached via `th:insert`/`th:replace`
from somewhere that supplies `row`. See `templates/home-fragments.html` for
a second worked example (the dashboard's own demo table and modal).

**3. Call the table**, pointing `rowFragment` at that fragment:

```html
<div th:replace="~{components/data-table :: dataTable(${columns}, ${rows},
        ~{catalogue/books-fragments :: bookRow}, ${currentSort}, ${currentDir},
        'book-open', 'No books match your filters.', null, null)}"></div>
```

That's it — sortable headers, sticky header, row hover, its own horizontal
scroll container, and the empty state when `rows` is empty are all handled
by the fragment and `app.css`. `emptyActionText`/`emptyActionHref` (the
last two args, both `null` above) are optional — set both to add a button
to the empty state, e.g. `'Add a book'`, `@{/catalogue/books/new}`.

**Why does the row fragment work without `(row)`?** A fragment reference
with no parameter list — `~{template :: fragmentName}`, no parentheses —
inherits every local variable visible at its call site, in Thymeleaf. The
call site is inside `data-table`'s own `th:each="row : ${rows}"`, so `row`
is in scope and your fragment can just read `${row}`. Give the fragment an
explicit parameter list instead (`bookRow(row)`) and this inheritance is
switched off — `row` would be undefined inside it. Leave the parentheses
off.

**Loading state before the rows arrive** (e.g. an HTMX swap): see
`skeleton-row.html`'s `skeletonTableRows` fragment below — reuse the same
`columns` list so the skeleton lines up with the real header.

**Known limitation:** the sort links only carry `?sort=…&dir=…` — they
don't preserve any other query parameters (a search box's `?q=…`, a
category filter). If your screen filters *and* sorts, build the header
links yourself rather than using this fragment's sort links as-is, or ask
before extending it — don't silently ship a page where changing the sort
order drops the user's filter.

---

## form-field

Must be used inside a `<form th:object="${yourCommand}">` — the fragment
binds by property name against whatever object that form is bound to.

```html
<form th:action="@{/catalogue/books/new}" th:object="${bookForm}" method="post">

    <div th:replace="~{components/form-field :: formField('title', 'Title', 'text',
            null, true, null, null, null)}"></div>

    <div th:replace="~{components/form-field :: formField('publicationYear', 'Publication year', 'number',
            'Leave blank if unknown.', false, null, null, 'e.g. 2024')}"></div>

    <div th:replace="~{components/form-field :: formField('memberType', 'Member type', 'select',
            null, true, ${memberTypeOptions}, null, null)}"></div>

    <div th:replace="~{components/form-field :: formField('description', 'Description', 'textarea',
            null, false, null, 5, null)}"></div>

    <button type="submit" class="btn btn--primary">Save</button>
</form>
```

where the controller supplied

```java
model.addAttribute("bookForm", new BookForm());
model.addAttribute("memberTypeOptions", List.of(
        new SelectOption("Student", "Student"),
        new SelectOption("Academic Staff", "Academic Staff")));
```

Validation errors show automatically — if `bookForm` is a `@Valid` command
object and binding failed, the field with the error gets the `--error`
border and its `form-field__error` message underneath, with no extra work
at the call site. This is Thymeleaf's own `#fields`/`#ids` machinery, not
something this fragment invents, so it also works correctly if the same
field name appears twice on one page (an inline-edit row repeated in a
`th:each`, say).

Parameter order: `field, label, type, help, required, options, textareaRows, placeholder`.
`type` defaults to `'text'` when `null`. Pass `null` for whichever of
`help` / `required` / `options` / `textareaRows` / `placeholder` don't
apply — see `form-field.html`'s own header comment for the full list of
`type` values.

---

## modal / confirm-dialog

Both use the same shared JS (`/static/js/app-shell.js` §4): open with a
button carrying `data-modal-trigger="theId"` anywhere on the page, or from
your own code with `LMS.modal.open('theId')`. Esc, the backdrop, and focus
return all already work — nothing to wire up.

**A form inside a modal** — `modal.html`, for anything that isn't a plain
yes/no confirmation:

```html
<button type="button" class="btn btn--secondary" data-modal-trigger="renew-loan-42">Renew</button>

<div th:replace="~{components/modal :: modal('renew-loan-42', 'Renew loan',
        ~{loans/detail-fragments :: renewalFormBody}, ~{loans/detail-fragments :: renewalFormFooter})}"></div>
```

```html
<!-- templates/loans/detail-fragments.html — a companion file, not inside
     detail.html itself; same reason as data-table's row fragment above. -->
<div th:fragment="renewalFormBody">
    <div th:replace="~{components/form-field :: formField('days', 'Extension (days)', 'number',
            'Maximum 2 renewals per loan.', true, null, null, null)}"></div>
</div>
<div th:fragment="renewalFormFooter">
    <button type="button" class="btn btn--secondary" data-modal-close>Cancel</button>
    <button type="submit" class="btn btn--primary">Confirm renewal</button>
</div>
```

(Same no-parentheses rule as `data-table`'s row fragment — `bodyFragment`
and `footerFragment` are both called without arguments, so they read the
page's own model directly. Same companion-file rule too: `loans/detail.html`
is a real page, so its own model's `demoForm`-style attributes would render
harmlessly a second time in place if these lived inside it directly — kept
separate to not depend on that being harmless every time.)

**A plain "are you sure?"** — `confirm-dialog.html`, for one action against
one URL, replacing `confirm()` (banned outright, design-system.md §7):

```html
<button type="button" class="btn btn--secondary" data-modal-trigger="confirm-write-off-17">Write off</button>

<div th:replace="~{components/confirm-dialog :: confirmDialog('confirm-write-off-17',
        'Write off this fine?',
        'The member will no longer owe LKR 500 for this incident. This cannot be undone.',
        'Write off', 'Cancel', @{/fines/17/write-off}, true)}"></div>
```

The last argument (`danger`) is `true` here because writing off a fine
can't be undone — it paints the Confirm button with the `--overdue` token
instead of `--primary`. Use `false` for a confirmation that's just "make
sure before we act on this", not destructive.

Each dialog targets one URL, so a row that can be deleted needs its own
dialog instance with that row's id already baked into both the fragment's
own `id` and its `confirmAction` — not one dialog shared across rows.

---

## toast

The JS already builds toast markup (`LMS.toast.show({title, message,
variant})`) for a click that doesn't leave the page — see
`home.html`'s "Component demo" panel for a trigger button. This fragment
covers the other case: showing a toast for the result of an action that
*did* redirect, via Spring's flash attributes.

```java
@PostMapping("/loans/{id}/renew")
public String renew(@PathVariable int id, RedirectAttributes redirectAttributes) {
    // ...
    redirectAttributes.addFlashAttribute("flashSuccessTitle", "Loan renewed");
    redirectAttributes.addFlashAttribute("flashSuccessMessage", "New due date: 29 Sep.");
    return "redirect:/loans/" + id;
}
```

```html
<!-- once, right after <body> opens in your page content -->
<div th:replace="~{components/toast :: flash(${flashSuccessTitle}, ${flashSuccessMessage},
                                              ${flashErrorTitle}, ${flashErrorMessage})}"></div>
```

All four are `null` on a normal (non-redirected) page load, so nothing
shows — the fragment only fires the script tags for whichever pair you
actually set.

---

## empty-state

Used standalone, and automatically by `data-table` when `rows` is empty:

```html
<div th:replace="~{components/empty-state :: emptyState('inbox', 'No fines on this account.',
                                                          'View borrowing history', @{/loans})}"></div>
```

`actionText` / `actionHref` are optional — pass `null` for both to show
just the icon and sentence.

---

## skeleton-row

```html
<!-- a dashboard card whose content loads after the page does -->
<div th:replace="~{components/skeleton-row :: skeletonRows(3)}"></div>
```

```html
<!-- a real data-table, while its rows are still loading -->
<table class="data-table">
    <thead>...</thead>
    <tbody th:replace="~{components/skeleton-row :: skeletonTableRows(${columns}, 5)}"></tbody>
</table>
```

Never a spinner as the only loading state (design-system.md §7) — this is
what replaces one.

---

## stat-tile

```html
<div th:replace="~{components/stat-tile :: statTileRow(${tiles})}"></div>
```

```java
model.addAttribute("tiles", List.of(
        new StatTile("Total Books", "1,248"),
        new StatTile("Active Loans", "312"),
        new StatTile("Overdue", "14", true)));   // true = paint the value in --overdue
```

Dashboards only — never inside a form or a list (design-system.md §5).

---

## pagination

```html
<div th:replace="~{components/pagination :: pagination(${page}, ${totalPages}, ${totalItems}, ${pageSize})}"></div>
```

`page` is 1-based. Same query-parameter caveat as `data-table`'s sort
links — see that section.
