package com.lms.user.dto;

import java.time.LocalDate;

public record AllMemberRow(Integer memberId, String name, String membershipNo, String email, String memberType,
        String membershipStatus, LocalDate joinedDate, boolean canSuspend, boolean canReactivate,
        boolean pendingApproval) {
}
