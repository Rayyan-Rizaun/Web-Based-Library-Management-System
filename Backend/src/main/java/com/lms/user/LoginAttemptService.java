package com.lms.user;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.common.domain.AppUser;
import com.lms.common.domain.AppUserRepository;
import com.lms.common.domain.AppUserStatus;
import com.lms.common.domain.FailedLoginAttempt;
import com.lms.common.domain.FailedLoginAttemptRepository;
import com.lms.common.domain.FailureReason;
import com.lms.common.domain.SystemSettingRepository;

/**
 * The account-lockout business rule (business-rules.md §8): every failed
 * login is recorded, and an account is locked after
 * {@code Security.LockoutMaxAttempts} failures for its email within the
 * last {@code Security.LockoutWindowMinutes} minutes. Both numbers are
 * read from {@code SystemSetting} (CLAUDE.md rule 9) — never hard-coded —
 * so "what if it were 10 in 30 minutes" is a one-row UPDATE, the same way
 * every other policy number in this project already works.
 *
 * <p>Called from {@code LoginFailureHandler}, which is what turns a raw
 * Spring Security {@code AuthenticationException} into the specific {@link
 * FailureReason} this service records.
 */
@Service
public class LoginAttemptService {

    private final FailedLoginAttemptRepository failedAttempts;
    private final AppUserRepository appUsers;
    private final SystemSettingRepository settings;

    public LoginAttemptService(FailedLoginAttemptRepository failedAttempts, AppUserRepository appUsers,
            SystemSettingRepository settings) {
        this.failedAttempts = failedAttempts;
        this.appUsers = appUsers;
        this.settings = settings;
    }

    /**
     * Records one failed attempt, then — only for {@link FailureReason#BadPassword}
     * against a real, still-active account — checks whether this attempt tips the
     * account into lockout and locks it if so (business-rules.md §8: an
     * UnknownEmail attempt has no account to lock; an already-locked or
     * already-deactivated account does not need locking again).
     */
    @Transactional
    public void recordFailure(String emailTried, HttpServletRequest request, FailureReason reason) {
        FailedLoginAttempt attempt = new FailedLoginAttempt();
        attempt.setEmailTried(normalise(emailTried));
        attempt.setIpAddress(clientIp(request));
        attempt.setUserAgent(request.getHeader("User-Agent"));
        attempt.setFailureReason(reason);
        failedAttempts.save(attempt);

        if (reason == FailureReason.BadPassword) {
            lockIfThresholdReached(normalise(emailTried));
        }
    }

    private void lockIfThresholdReached(String email) {
        Optional<AppUser> account = appUsers.findByEmailIgnoreCase(email);
        if (account.isEmpty() || account.get().getStatus() != AppUserStatus.Active) {
            return;
        }

        int maxAttempts = settings.findById("Security.LockoutMaxAttempts")
                .map(s -> Integer.parseInt(s.getSettingValue())).orElse(5);
        int windowMinutes = settings.findById("Security.LockoutWindowMinutes")
                .map(s -> Integer.parseInt(s.getSettingValue())).orElse(15);

        LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);
        long recentFailures = failedAttempts.countByEmailTriedIgnoreCaseAndAttemptedAtAfter(email, since);

        if (recentFailures >= maxAttempts) {
            AppUser user = account.get();
            user.setStatus(AppUserStatus.Locked);
            appUsers.save(user);
        }
    }

    private static String normalise(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Honours a reverse proxy's X-Forwarded-For; falls back to the direct remote address. */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
