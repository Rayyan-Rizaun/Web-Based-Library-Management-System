package com.lms.user;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.lms.common.domain.FailureReason;

/**
 * Runs on every failed login, whatever the reason, and is the one place
 * that turns a raw Spring Security {@link AuthenticationException} into
 * one of the four {@link FailureReason} values {@code FailedLoginAttempt}
 * actually stores.
 *
 * <p>The exception type IS the reason, because {@code AppUserDetailsService}
 * / {@code AppUserPrincipal} already report it precisely:
 * <ul>
 *   <li>{@link UsernameNotFoundException} — no account for that email.</li>
 *   <li>{@link LockedException} — {@code AppUserPrincipal.isAccountNonLocked()}
 *       returned false (a status of {@code Locked}).</li>
 *   <li>{@link DisabledException} — {@code isEnabled()} returned false (a
 *       status of {@code Deactivated}).</li>
 *   <li>anything else — a real account, correctly identified, wrong
 *       password.</li>
 * </ul>
 * This only works because {@code SecurityConfig} sets {@code
 * hideUserNotFoundExceptions(false)}, so an unknown email is not silently
 * folded into "bad credentials" the way Spring Security defaults to (that
 * default exists to stop username enumeration; this project accepts that
 * trade-off for an accurate audit trail — see {@code SecurityConfig}'s own
 * comment).
 */
@Component
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    public LoginFailureHandler(LoginAttemptService loginAttemptService) {
        super("/login?error");
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("email");
        FailureReason reason = reasonFor(exception);
        loginAttemptService.recordFailure(email, request, reason);

        // The login page reads ?error=<reason> to show one honest message
        // ("this account is locked") without ever revealing, for a wrong
        // password, whether the difference was the email or the password.
        setDefaultFailureUrl("/login?error=" + reason.name());
        super.onAuthenticationFailure(request, response, exception);
    }

    private static FailureReason reasonFor(AuthenticationException exception) {
        if (exception instanceof UsernameNotFoundException) {
            return FailureReason.UnknownEmail;
        }
        if (exception instanceof LockedException) {
            return FailureReason.AccountLocked;
        }
        if (exception instanceof DisabledException) {
            return FailureReason.AccountDeactivated;
        }
        return FailureReason.BadPassword;
    }
}
