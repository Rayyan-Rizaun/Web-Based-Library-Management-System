package com.lms.user.dto;

import com.lms.common.domain.Member;
import com.lms.common.domain.StaffProfile;

/**
 * Everything {@code account.html} needs, assembled in one place by {@code
 * AccountService} so the template does no repository work of its own
 * (CLAUDE.md rule 5 — controllers/templates don't hold business logic, and
 * "what does this page show" is exactly that kind of decision).
 *
 * <p>{@code member} / {@code staff} are {@code null} when the signed-in
 * user has no such row — the overlapping ISA (R30) means both, either, or
 * neither can be true for one account.
 */
public record AccountView(String fullName, String email, String phone, Member member, StaffProfile staff) {

    /** MembershipStatus.Suspended, used at registration as "awaiting librarian approval" (business-rules.md §8). */
    public boolean isPendingApproval() {
        return member != null && member.getMembershipStatus() == com.lms.common.domain.MembershipStatus.Suspended;
    }
}
