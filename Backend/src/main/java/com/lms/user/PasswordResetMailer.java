package com.lms.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Where the password-reset email would be sent from — deliberately not
 * real mail. This phase's explicit scope is to log the token to the
 * console rather than send it, so this class is the one place that
 * decision lives: swapping in a real {@code JavaMailSender} later means
 * replacing this class's body, not hunting through {@link
 * PasswordResetService} for where "sending" happens.
 */
@Component
public class PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);

    public void sendResetLink(String toEmail, String resetLink) {
        log.info("Password reset requested for {} — link (would be emailed): {}", toEmail, resetLink);
    }
}
