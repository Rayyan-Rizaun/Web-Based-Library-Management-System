package com.lms.common.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.common.domain.AppUserRepository;

/**
 * Loads the Spring Security principal from {@code AppUser} by email.
 *
 * <p>Throws the specific {@link UsernameNotFoundException} rather than a
 * generic failure, and {@code SecurityConfig} is configured with {@code
 * hideUserNotFoundExceptions(false)} so this exception reaches {@code
 * com.lms.user.LoginFailureHandler} distinctly from a wrong password — see
 * that class for why: it is what lets a failed attempt be recorded as
 * {@code FailureReason.UnknownEmail} rather than lumping every failure
 * together as a bad password.
 *
 * <p>{@code @Transactional}: {@code AppUser.roleAssignments} is lazy
 * (loaded by {@link AppUserPrincipal}'s constructor), and the persistence
 * context must still be open when that happens.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUsers;

    public AppUserDetailsService(AppUserRepository appUsers) {
        this.appUsers = appUsers;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return appUsers.findByEmailIgnoreCase(email)
                .map(AppUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + email));
    }
}
