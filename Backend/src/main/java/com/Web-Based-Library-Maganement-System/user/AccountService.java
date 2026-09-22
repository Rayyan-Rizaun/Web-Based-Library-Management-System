package com.lms.user;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.common.domain.AppUser;
import com.lms.common.domain.AppUserRepository;
import com.lms.common.domain.MemberRepository;
import com.lms.common.domain.StaffProfileRepository;
import com.lms.user.dto.AccountView;

/**
 * Assembles the "My Account" page's data. Looks {@code Member} and {@code
 * StaffProfile} up separately by {@code UserID} rather than through
 * {@code AppUser} (which has no association to either, by design — see
 * that entity's javadoc) — the same pattern {@code AppUserPrincipal}
 * already follows.
 */
@Service
public class AccountService {

    private final AppUserRepository appUsers;
    private final MemberRepository members;
    private final StaffProfileRepository staffProfiles;

    public AccountService(AppUserRepository appUsers, MemberRepository members, StaffProfileRepository staffProfiles) {
        this.appUsers = appUsers;
        this.members = members;
        this.staffProfiles = staffProfiles;
    }

    /**
     * {@code isAuthenticated()} is already enforced at the URL level (see
     * {@code SecurityConfig}: {@code anyRequest().authenticated()}) — this
     * is belt-and-suspenders so the check travels with the service method
     * itself and still holds if it is ever called from somewhere new that
     * forgets to check first.
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public AccountView viewFor(Integer userId) {
        AppUser user = appUsers.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Signed-in user " + userId + " no longer exists"));

        return new AccountView(
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                members.findByUserUserId(userId).orElse(null),
                staffProfiles.findByUserUserId(userId).orElse(null));
    }
}
