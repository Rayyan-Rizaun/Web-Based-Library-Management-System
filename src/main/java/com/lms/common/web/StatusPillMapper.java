package com.lms.common.web;

import com.lms.common.domain.DbValueEnum;
import org.springframework.stereotype.Component;

/**
 * Maps every status enumeration in the database onto one of the six status
 * tokens design-system.md §1 defines — {@code available}, {@code on-loan},
 * {@code due-soon}, {@code overdue}, {@code reserved}, {@code lost}. Those
 * six colours are it; the design system bans introducing a seventh, so a
 * fourteenth status domain still has to fit one of the six existing hues.
 *
 * <p>Used from {@code components/status-pill.html} as
 * {@code @statusPill.token(domain, status)}. Registered under the bean name
 * {@code statusPill} so that expression resolves.
 *
 * <p><b>The six hues, and what each one means here</b> (not literally what
 * their names say — "on-loan" is reused for "currently active", "reserved"
 * for "waiting on someone else's decision"):
 * <ul>
 *   <li>{@code available} — settled in the account's favour: active and in
 *       good standing, returned, approved, fulfilled, paid, resolved.</li>
 *   <li>{@code on-loan} — currently in progress, ongoing, nothing wrong.</li>
 *   <li>{@code due-soon} — action needed, usually against a clock (a hold
 *       expiring, a balance still owed, a copy being repaired).</li>
 *   <li>{@code overdue} — actively blocking or a failure: locked accounts,
 *       rejections, failed payments, an expired or suspended membership.</li>
 *   <li>{@code reserved} — waiting in a queue for someone else to act:
 *       pending approvals, an open incident or flag, a title on hold.</li>
 *   <li>{@code lost} — gone, archived, written off: physically lost or
 *       damaged copies, withdrawn stock, a cancelled reservation, a
 *       deactivated account, a hidden or removed review.</li>
 * </ul>
 *
 * <p>Two values below are never stored — Loan's {@code Overdue} and
 * Member's {@code Expired} are both derived from a date rather than a
 * column (R15, R26 in database/00_relational_mapping.md). A service
 * computes them; this mapper still needs to colour them once it does.
 */
@Component("statusPill")
public class StatusPillMapper {

    private static final String AVAILABLE = "available";
    private static final String ON_LOAN = "on-loan";
    private static final String DUE_SOON = "due-soon";
    private static final String OVERDUE = "overdue";
    private static final String RESERVED = "reserved";
    private static final String LOST = "lost";

    /**
     * @param domain one of the constants documented in
     *               templates/components/README.md (e.g. {@code "loan"},
     *               {@code "fine"}, {@code "bookCopy"}) — one per status
     *               enumeration in the database.
     * @param status the raw status. Accepts a {@link DbValueEnum} (calls
     *               {@link DbValueEnum#dbValue()}), a plain {@link Enum}
     *               (calls {@code name()}), or any other object (calls
     *               {@code toString()}) — so a caller can pass an entity's
     *               status field exactly as it comes off the entity,
     *               whichever of the two enum styles that column uses, or a
     *               plain derived {@link String} like {@code "Overdue"}.
     * @return the CSS suffix for {@code status-pill--<suffix>}.
     */
    /**
     * The human-readable text for a status, for the pill's own label — e.g.
     * {@code BookCopyStatus.OnLoan.dbValue()} is {@code "On Loan"}, not the
     * Java constant name {@code toString()} would otherwise give
     * ({@code "OnLoan"}). Same three-way dispatch as {@link #token}, so the
     * pill's colour and its text can never name two different statuses.
     */
    public String displayValue(Object status) {
        return status == null ? "" : rawValue(status);
    }

    public String token(String domain, Object status) {
        if (status == null) {
            throw new IllegalArgumentException("statusPill.token: status is null for domain '" + domain + "'");
        }
        String value = rawValue(status);
        return switch (domain) {
            case "appUser" -> appUser(value);
            case "membership" -> membership(value);
            case "bookCopy" -> bookCopy(value);
            case "loan" -> loan(value);
            case "renewal" -> renewal(value);
            case "reservation" -> reservation(value);
            case "incident" -> incident(value);
            case "fine" -> fine(value);
            case "appeal" -> appeal(value);
            case "payment" -> payment(value);
            case "review" -> review(value);
            case "flag" -> flag(value);
            case "feedback" -> feedback(value);
            case "backup" -> backup(value);
            default -> throw unknownDomain(domain);
        };
    }

    private static String rawValue(Object status) {
        if (status instanceof DbValueEnum dbValueEnum) {
            return dbValueEnum.dbValue();
        }
        if (status instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return status.toString();
    }

    // ---- AppUser.Status (CK_AppUser_Status) ------------------------------
    private String appUser(String value) {
        return switch (value) {
            case "Active" -> AVAILABLE;
            case "Locked" -> OVERDUE;         // blocked from logging in
            case "Deactivated" -> LOST;        // administratively retired
            default -> throw unknownValue("appUser", value);
        };
    }

    // ---- Member.MembershipStatus (CK_Member_MembershipStatus) ------------
    // "Expired" is derived from ExpiryDate (R26), never stored.
    private String membership(String value) {
        return switch (value) {
            case "Active" -> AVAILABLE;
            case "Suspended" -> OVERDUE;       // blocks borrowing, like Locked
            case "Expired" -> OVERDUE;         // derived — blocks borrowing (business-rules §4)
            case "Cancelled" -> LOST;
            default -> throw unknownValue("membership", value);
        };
    }

    // ---- BookCopy.Status (CK_BookCopy_Status) -----------------------------
    private String bookCopy(String value) {
        return switch (value) {
            case "Available" -> AVAILABLE;
            case "On Loan" -> ON_LOAN;
            case "On Hold" -> RESERVED;        // held for a reservation
            case "Under Repair" -> DUE_SOON;   // temporarily out, expected back
            case "Damaged", "Lost", "Withdrawn" -> LOST;
            default -> throw unknownValue("bookCopy", value);
        };
    }

    // ---- Loan.Status (CK_Loan_Status) -------------------------------------
    // "Overdue" is derived: ReturnedAt IS NULL AND DueAt < now (R15).
    private String loan(String value) {
        return switch (value) {
            case "Active" -> ON_LOAN;
            case "Overdue" -> OVERDUE;         // derived
            case "Returned" -> AVAILABLE;
            case "Lost" -> LOST;
            default -> throw unknownValue("loan", value);
        };
    }

    // ---- LoanRenewal.Status (CK_LoanRenewal_Status) -----------------------
    private String renewal(String value) {
        return decision(value, "renewal");
    }

    // ---- FineAppeal.Status (CK_FineAppeal_Status) -------------------------
    private String appeal(String value) {
        return decision(value, "appeal");
    }

    /** Renewal and Appeal share the same Pending/Approved/Rejected shape. */
    private String decision(String value, String domainForError) {
        return switch (value) {
            case "Pending" -> RESERVED;        // waiting on staff
            case "Approved" -> AVAILABLE;
            case "Rejected" -> OVERDUE;
            default -> throw unknownValue(domainForError, value);
        };
    }

    // ---- Reservation.Status (CK_Reservation_Status) -----------------------
    private String reservation(String value) {
        return switch (value) {
            case "Waiting" -> RESERVED;
            case "Ready" -> DUE_SOON;          // 3-day collection window ticking
            case "Fulfilled" -> AVAILABLE;
            case "Cancelled", "Expired" -> LOST;   // missed slot — not coming back
            default -> throw unknownValue("reservation", value);
        };
    }

    // ---- BookIncident.Status (CK_BookIncident_Status) ---------------------
    private String incident(String value) {
        return switch (value) {
            case "Open" -> RESERVED;           // reported, awaiting action
            case "Charged" -> DUE_SOON;         // fine raised, payment owed
            case "Resolved" -> AVAILABLE;
            case "Written Off" -> LOST;
            default -> throw unknownValue("incident", value);
        };
    }

    // ---- Fine.Status (CK_Fine_Status) --------------------------------------
    private String fine(String value) {
        return switch (value) {
            case "Pending", "Partially Paid" -> DUE_SOON;   // money still owed
            case "Under Appeal" -> RESERVED;                // paused for a decision
            case "Fully Paid", "Waived" -> AVAILABLE;        // settled, either way
            default -> throw unknownValue("fine", value);
        };
    }

    // ---- FinePayment.PaymentStatus (CK_FinePayment_PaymentStatus) --------
    private String payment(String value) {
        return switch (value) {
            case "Completed" -> AVAILABLE;
            case "Refunded" -> DUE_SOON;        // reversed — flagged for follow-up
            case "Failed" -> OVERDUE;
            default -> throw unknownValue("payment", value);
        };
    }

    // ---- BookReview.Status (CK_BookReview_Status) --------------------------
    // Also ReviewModerationHistory.PreviousStatus / NewStatus — same domain.
    private String review(String value) {
        return switch (value) {
            case "Pending" -> RESERVED;         // awaiting moderation
            case "Approved" -> AVAILABLE;
            case "Rejected" -> OVERDUE;
            case "Hidden", "Removed" -> LOST;
            default -> throw unknownValue("review", value);
        };
    }

    // ---- ReviewFlag.Status (CK_ReviewFlag_Status) --------------------------
    private String flag(String value) {
        return switch (value) {
            case "Open" -> RESERVED;            // awaiting a moderator decision
            case "Dismissed" -> AVAILABLE;       // review found to be fine
            case "Upheld" -> OVERDUE;            // violation confirmed
            default -> throw unknownValue("flag", value);
        };
    }

    // ---- MemberFeedback.Status (CK_MemberFeedback_Status) -----------------
    // Also FeedbackHistory.PreviousStatus / NewStatus — same domain.
    private String feedback(String value) {
        return switch (value) {
            case "Submitted" -> RESERVED;        // newly filed, awaiting review
            case "Under Review", "In Progress" -> ON_LOAN;   // actively being handled
            case "Resolved" -> AVAILABLE;
            case "Closed" -> LOST;               // archived, whether or not resolved
            default -> throw unknownValue("feedback", value);
        };
    }

    // ---- DatabaseBackupLog.Status (CK_DatabaseBackupLog_Status) -----------
    private String backup(String value) {
        return switch (value) {
            case "Running" -> ON_LOAN;
            case "Succeeded" -> AVAILABLE;
            case "Failed" -> OVERDUE;
            default -> throw unknownValue("backup", value);
        };
    }

    private static IllegalStateException unknownDomain(String domain) {
        return new IllegalStateException(
                "statusPill: unknown domain '" + domain + "'. Valid domains are listed in "
                        + "templates/components/README.md and StatusPillMapper.token(). "
                        + "Add a case there rather than guessing a token in the template.");
    }

    private static IllegalStateException unknownValue(String domain, String value) {
        return new IllegalStateException(
                "statusPill: no mapping for domain '" + domain + "', value '" + value + "'. "
                        + "If this is a new CHECK-constrained value in 01_schema.sql, add a case "
                        + "to StatusPillMapper." + domain + "(String) — never guess a colour in the template.");
    }
}
