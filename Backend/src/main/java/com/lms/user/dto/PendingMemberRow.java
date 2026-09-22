package com.lms.user.dto;

import java.time.LocalDate;

public record PendingMemberRow(Integer memberId, String name, String membershipNo, String email,
        String nationalId, String phone, String memberType, LocalDate joinedDate) {
}
