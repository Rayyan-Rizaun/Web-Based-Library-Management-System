package com.lms.admin.dto;

import java.time.LocalDateTime;

public record FailedLoginRow(Long attemptId, String emailTried, String reason, String ipAddress,
        LocalDateTime attemptedAt) {
}
