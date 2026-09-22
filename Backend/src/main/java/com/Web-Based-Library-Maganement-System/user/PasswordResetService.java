package com.lms.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.lms.common.domain.AppUser;
import com.lms.common.domain.AppUserRepository;
import com.lms.common.domain.PasswordResetToken;
import com.lms.common.domain.PasswordResetTokenRepository;
import com.lms.common.domain.SystemSettingRepository;
import com.lms.common.security.AuditAction;

/**
 * UC-01 "forgot password" flow: request a reset link, then use it once to
 * set a new password. See business-rules.md §8 for the policy numbers
 * (30-minute link validity, single use) and {@code
 * database/01_schema.sql}'s comment on {@code PasswordResetToken} for why
 * only a hash of the token is ever stored.
 */
@Service
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository appUsers;
    private final PasswordResetTokenRepository tokens;
    private final SystemSettingRepository settings;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailer mailer;

    public PasswordResetService(AppUserRepository appUsers, PasswordResetTokenRepository tokens,
            SystemSettingRepository settings, PasswordEncoder passwordEncoder, PasswordResetMailer mailer) {
        this.appUsers = appUsers;
        this.tokens = tokens;
        this.settings = settings;
        this.passwordEncoder = passwordEncoder;
        this.mailer = mailer;
    }

    /**
     * Generates and stores a token for {@code email}, if an account with
     * that email exists — and does nothing, silently, if it does not.
     * Either way the caller (the controller) shows the identical
     * confirmation message; this is what stops the forgot-password form
     * from being usable to check which emails are registered, which the
     * login form (business-rules.md §8) deliberately does not bother
     * doing for the sake of an accurate audit trail. A password-reset
     * request has no equivalent audit need, so here the safer default
     * applies.
     */
    @Transactional
    public void requestReset(String email, HttpServletRequest request) {
        Optional<AppUser> account = appUsers.findByEmailIgnoreCase(email);
        if (account.isEmpty()) {
            return;
        }
        AppUser user = account.get();

        String rawToken = generateRawToken();
        int validMinutes = settings.findById("Security.PasswordResetTokenMinutes")
                .map(s -> Integer.parseInt(s.getSettingValue())).orElse(30);

        PasswordResetToken token = new PasswordResetToken(
                user, sha256Hex(rawToken), LocalDateTime.now().plusMinutes(validMinutes), request.getRemoteAddr());
        tokens.save(token);

        String resetLink = ServletUriComponentsBuilder.fromContextPath(request)
                .path("/reset-password")
                .queryParam("token", rawToken)
                .toUriString();
        mailer.sendResetLink(user.getEmail(), resetLink);
    }

    /** True if {@code rawToken} matches a stored token that is neither used nor expired. */
    @Transactional(readOnly = true)
    public boolean isValid(String rawToken) {
        return findUsable(rawToken).isPresent();
    }

    @AuditAction(action = "UPDATE", entity = "AppUser")
    @Transactional
    public AppUser resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = findUsable(rawToken)
                .orElseThrow(() -> new IllegalStateException("Reset link is invalid or has expired"));

        AppUser user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        appUsers.save(user);

        token.setConsumedAt(LocalDateTime.now());
        tokens.save(token);

        return user;
    }

    private Optional<PasswordResetToken> findUsable(String rawToken) {
        return tokens.findByTokenHashAndConsumedAtIsNull(sha256Hex(rawToken))
                .filter(token -> token.isUsable(LocalDateTime.now()));
    }

    /** 32 random bytes, URL-safe base64 — long enough that guessing it is not a realistic attack. */
    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a mandatory algorithm for every JDK provider (JLS/JCA guarantee).
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
