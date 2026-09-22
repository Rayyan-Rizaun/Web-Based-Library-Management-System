package com.lms.common.security;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.lms.common.domain.AppUser;
import com.lms.common.domain.AppUserStatus;

/**
 * The Spring Security principal for a signed-in {@link AppUser}.
 *
 * <p>Deliberately holds only what login itself needs — the user's id,
 * name, password hash and granted authorities — not their {@code Member}
 * or {@code StaffProfile} row. {@code AppUser} itself has no association
 * to either, on purpose (see its javadoc): whichever screen needs "is this
 * user a member" or "is this user staff" looks that up with {@code
 * MemberRepository.findByUserUserId} / {@code
 * StaffProfileRepository.findByUserUserId}, the same way every other
 * feature does. Loading them here would mean two extra queries on every
 * login for data most requests never use.
 *
 * <p>Authorities are the user's {@code Role.RoleName} values verbatim, with
 * no {@code ROLE_} prefix — see business-rules.md §8 for why
 * {@code hasAuthority(...)}, not {@code hasRole(...)}, is used everywhere
 * these are checked.
 */
public final class AppUserPrincipal implements UserDetails {

    private final Integer userId;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final AppUserStatus status;
    private final Set<SimpleGrantedAuthority> authorities;

    public AppUserPrincipal(AppUser user) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.fullName = user.getFirstName() + " " + user.getLastName();
        this.status = user.getStatus();
        this.authorities = user.getRoleAssignments().stream()
                .map(userRole -> new SimpleGrantedAuthority(userRole.getRole().getRoleName()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public Integer userId() {
        return userId;
    }

    public String fullName() {
        return fullName;
    }

    /** True if any granted authority matches one of the given role names exactly. */
    public boolean hasAnyRole(String... roleNames) {
        for (String roleName : roleNames) {
            if (authorities.contains(new SimpleGrantedAuthority(roleName))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    /** The login username is the email — {@code AppUser}'s alternate key. */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != AppUserStatus.Locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status != AppUserStatus.Deactivated;
    }
}
