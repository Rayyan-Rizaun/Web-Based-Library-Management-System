package com.lms.user;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.common.domain.AuditLog;
import com.lms.common.domain.AuditLogRepository;
import com.lms.common.domain.LoanRepository;
import com.lms.common.domain.LoanStatus;
import com.lms.common.domain.Member;
import com.lms.common.domain.MemberRepository;
import com.lms.common.domain.MemberType;
import com.lms.common.domain.MembershipStatus;
import com.lms.common.domain.NotificationFactory;
import com.lms.common.domain.NotificationRepository;
import com.lms.common.security.AuditAction;
import com.lms.common.web.SelectOption;
import com.lms.user.dto.AllMemberRow;
import com.lms.user.dto.PendingMemberRow;
import com.lms.user.dto.RejectMemberForm;
import com.lms.user.dto.SuspendMemberForm;

/**
 * Backlog item PB-09 — member approval. {@code MembershipStatus.Suspended}
 * is already overloaded by self-registration to mean "awaiting approval"
 * (business-rules.md §8), and is reused again here to mean "suspended by
 * staff" for an {@link #suspend} action, so the two cases are told apart by
 * whether a {@code SUSPEND} {@link AuditLog} row exists for the member —
 * a fresh self-registration has never had one, a staff suspension always
 * does. {@link #wasStaffSuspended} is the one place that check lives;
 * every list method here calls it rather than trusting the raw status
 * column alone.
 */
@Service
@Transactional
public class MemberAdminService {

    private static final String ADMIN_ROLE = "hasAuthority('Library Administrator')";

    static final int PAGE_SIZE = 20;

    private final MemberRepository members;
    private final LoanRepository loans;
    private final NotificationRepository notifications;
    private final NotificationFactory notificationFactory;
    private final AuditLogRepository auditLogs;

    public MemberAdminService(MemberRepository members, LoanRepository loans, NotificationRepository notifications,
            NotificationFactory notificationFactory, AuditLogRepository auditLogs) {
        this.members = members;
        this.loans = loans;
        this.notifications = notifications;
        this.notificationFactory = notificationFactory;
        this.auditLogs = auditLogs;
    }

    public static List<SelectOption> statusFilterOptions() {
        List<SelectOption> options = new ArrayList<>();
        options.add(new SelectOption("", "All statuses"));
        for (MembershipStatus status : MembershipStatus.values()) {
            options.add(new SelectOption(status.name(), status.name()));
        }
        return options;
    }

    public static List<SelectOption> memberTypeFilterOptions() {
        List<SelectOption> options = new ArrayList<>();
        options.add(new SelectOption("", "All member types"));
        for (MemberType type : MemberType.values()) {
            options.add(new SelectOption(type.name(), type.dbValue()));
        }
        return options;
    }

    // ================= Pending / Rejected =================

    @PreAuthorize(ADMIN_ROLE)
    @Transactional(readOnly = true)
    public List<PendingMemberRow> pendingMembers() {
        return members.findByMembershipStatusOrderByJoinedDateAsc(MembershipStatus.Suspended).stream()
                .filter(m -> !wasStaffSuspended(m))
                .map(this::toPendingRow)
                .toList();
    }

    @PreAuthorize(ADMIN_ROLE)
    @Transactional(readOnly = true)
    public List<PendingMemberRow> rejectedMembers() {
        return members.findByMembershipStatusOrderByJoinedDateAsc(MembershipStatus.Cancelled).stream()
                .map(this::toPendingRow)
                .toList();
    }

    @PreAuthorize(ADMIN_ROLE)
    @Transactional(readOnly = true)
    public long pendingCount() {
        return members.findByMembershipStatusOrderByJoinedDateAsc(MembershipStatus.Suspended).stream()
                .filter(m -> !wasStaffSuspended(m))
                .count();
    }

    private PendingMemberRow toPendingRow(Member member) {
        return new PendingMemberRow(member.getMemberId(), fullNameOf(member), member.getMembershipNo(),
                member.getUser().getEmail(), member.getNationalId(), member.getUser().getPhone(),
                member.getMemberType().dbValue(), member.getJoinedDate());
    }

    /** APPROVE: business-rules.md §4, "membership validity: one academic year." */
    @PreAuthorize(ADMIN_ROLE)
    @AuditAction(action = "APPROVE", entity = "Member")
    public Member approve(Integer memberId) {
        Member member = members.findById(memberId).orElseThrow(() -> new NoSuchElementException("Member not found"));
        if (member.getMembershipStatus() != MembershipStatus.Suspended || wasStaffSuspended(member)) {
            throw new MemberAdminException("Only a pending registration can be approved.");
        }
        member.setMembershipStatus(MembershipStatus.Active);
        member.setExpiryDate(LocalDate.now().plusYears(1));
        members.save(member);
        notifications.save(notificationFactory.membershipApproved(member.getUser(), member.getExpiryDate()));
        return member;
    }

    @PreAuthorize(ADMIN_ROLE)
    @AuditAction(action = "REJECT", entity = "Member")
    public Member reject(Integer memberId, RejectMemberForm form) {
        Member member = members.findById(memberId).orElseThrow(() -> new NoSuchElementException("Member not found"));
        if (member.getMembershipStatus() != MembershipStatus.Suspended || wasStaffSuspended(member)) {
            throw new MemberAdminException("Only a pending registration can be rejected.");
        }
        member.setMembershipStatus(MembershipStatus.Cancelled);
        members.save(member);
        notifications.save(notificationFactory.membershipRejected(member.getUser(), form.getReason().trim()));
        return member;
    }

    // ================= All Members =================

    @PreAuthorize(ADMIN_ROLE)
    @Transactional(readOnly = true)
    public Page<AllMemberRow> allMembers(String q, String status, String memberType, int page, String sort, String dir) {
        String likeQuery = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        MembershipStatus statusFilter = (status == null || status.isBlank()) ? null : MembershipStatus.valueOf(status);
        MemberType typeFilter = (memberType == null || memberType.isBlank()) ? null : MemberType.valueOf(memberType);
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), PAGE_SIZE, sortFor(sort, dir));
        return members.searchAll(statusFilter, typeFilter, likeQuery, pageable).map(this::toAllMemberRow);
    }

    /** Only real {@link Member} columns are sortable — the user's name is a join, same restraint as {@code FineService.sortFor}. */
    private static Sort sortFor(String sort, String dir) {
        String property = switch (sort == null ? "" : sort) {
            case "membershipNo" -> "membershipNo";
            case "status" -> "membershipStatus";
            case "type" -> "memberType";
            default -> "joinedDate";
        };
        return Sort.by("desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC, property);
    }

    private AllMemberRow toAllMemberRow(Member member) {
        boolean staffSuspended = member.getMembershipStatus() == MembershipStatus.Suspended && wasStaffSuspended(member);
        boolean pendingApproval = member.getMembershipStatus() == MembershipStatus.Suspended && !staffSuspended;
        boolean canSuspend = member.getMembershipStatus() == MembershipStatus.Active;
        return new AllMemberRow(member.getMemberId(), fullNameOf(member), member.getMembershipNo(),
                member.getUser().getEmail(), member.getMemberType().dbValue(), member.getMembershipStatus().name(),
                member.getJoinedDate(), canSuspend, staffSuspended, pendingApproval);
    }

    /** business-rules.md §4 borrowing eligibility depends on an Active membership; a member with active loans keeps that membership until they are returned. */
    @PreAuthorize(ADMIN_ROLE)
    @AuditAction(action = "SUSPEND", entity = "Member")
    public Member suspend(Integer memberId, SuspendMemberForm form) {
        Member member = members.findById(memberId).orElseThrow(() -> new NoSuchElementException("Member not found"));
        if (member.getMembershipStatus() != MembershipStatus.Active) {
            throw new MemberAdminException("Only an active member can be suspended.");
        }
        long activeLoans = loans.countByMemberMemberIdAndStatus(memberId, LoanStatus.Active);
        if (activeLoans > 0) {
            throw new MemberAdminException(
                    "This member has " + activeLoans + " active loan(s) and cannot be suspended until they are returned.");
        }
        member.setMembershipStatus(MembershipStatus.Suspended);
        members.save(member);
        notifications.save(notificationFactory.membershipSuspended(member.getUser(), form.getReason().trim()));
        return member;
    }

    @PreAuthorize(ADMIN_ROLE)
    @AuditAction(action = "REACTIVATE", entity = "Member")
    public Member reactivate(Integer memberId) {
        Member member = members.findById(memberId).orElseThrow(() -> new NoSuchElementException("Member not found"));
        if (member.getMembershipStatus() != MembershipStatus.Suspended || !wasStaffSuspended(member)) {
            throw new MemberAdminException(
                    "Only a member suspended by staff can be reactivated here — a pending registration must be approved or rejected instead.");
        }
        member.setMembershipStatus(MembershipStatus.Active);
        members.save(member);
        notifications.save(notificationFactory.membershipReactivated(member.getUser()));
        return member;
    }

    /** Told apart from a still-pending self-registration by whether a {@code SUSPEND} action was ever logged against this member — see this class's own javadoc. */
    private boolean wasStaffSuspended(Member member) {
        return auditLogs.findByEntityNameAndEntityIdOrderByOccurredAtDesc("Member", member.getMemberId().toString())
                .stream()
                .anyMatch(a -> "SUSPEND".equals(a.getActionName()));
    }

    private static String fullNameOf(Member member) {
        return member.getUser().getFirstName() + " " + member.getUser().getLastName();
    }
}
