package com.lms.common.web;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.lms.common.domain.AuditLogRepository;
import com.lms.common.domain.MemberRepository;
import com.lms.common.domain.MembershipStatus;
import com.lms.common.security.AppUserPrincipal;

/**
 * Adds the signed-in user's name, id and role names to every view's model,
 * so shared fragments — {@code components/sidebar.html} (role-aware
 * navigation), {@code components/topbar.html} (the user menu) — can read
 * them without every controller repeating this. Same pattern as {@link
 * CurrentPathAdvice}, which does the same thing for the current URL.
 *
 * <p>{@code currentRoleNames} holds the exact {@code Role.RoleName}
 * strings ("Librarian", "Library Administrator", …) — see business-rules.md
 * §8 on why authorities are the database value verbatim, with no
 * {@code ROLE_} prefix. A template checks one with, e.g.,
 * {@code ${currentRoleNames.contains('Librarian')}}.
 *
 * <p>All three attributes are safe defaults (not authenticated, "Guest",
 * an empty role set) when there is no signed-in user, so every template
 * that reads them renders correctly for an anonymous visitor too.
 */
@ControllerAdvice
public class CurrentUserAdvice {

    private final MemberRepository members;
    private final AuditLogRepository auditLogs;

    public CurrentUserAdvice(MemberRepository members, AuditLogRepository auditLogs) {
        this.members = members;
        this.auditLogs = auditLogs;
    }

    /**
     * The Users &amp; Roles sidebar link's count badge — Library
     * Administrator only, otherwise the query is skipped entirely. A
     * Suspended member is "pending" rather than staff-suspended by the same
     * test {@code com.lms.user.MemberAdminService} uses on its own pending
     * list: no {@code SUSPEND} {@link com.lms.common.domain.AuditLog} row
     * against them yet. Duplicated here rather than called from that
     * service — this class lives in the shared foundation, and depending
     * on a feature package's service would invert CLAUDE.md rule 2's
     * layering the other way round.
     */
    @ModelAttribute("pendingMemberCount")
    public long pendingMemberCount() {
        AppUserPrincipal principal = principal();
        if (principal == null || principal.getAuthorities().stream()
                .noneMatch(a -> "Library Administrator".equals(a.getAuthority()))) {
            return 0;
        }
        return members.findByMembershipStatusOrderByJoinedDateAsc(MembershipStatus.Suspended).stream()
                .filter(m -> auditLogs.findByEntityNameAndEntityIdOrderByOccurredAtDesc("Member", m.getMemberId().toString())
                        .stream().noneMatch(a -> "SUSPEND".equals(a.getActionName())))
                .count();
    }

    @ModelAttribute("authenticated")
    public boolean authenticated() {
        return principal() != null;
    }

    @ModelAttribute("currentUserId")
    public Integer currentUserId() {
        AppUserPrincipal principal = principal();
        return principal == null ? null : principal.userId();
    }

    @ModelAttribute("currentUserName")
    public String currentUserName() {
        AppUserPrincipal principal = principal();
        return principal == null ? "Guest" : principal.fullName();
    }

    @ModelAttribute("currentRoleNames")
    public Set<String> currentRoleNames() {
        AppUserPrincipal principal = principal();
        if (principal == null) {
            return Set.of();
        }
        return principal.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static AppUserPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            return null;
        }
        return principal;
    }
}
