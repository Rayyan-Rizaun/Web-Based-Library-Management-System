package com.lms.common.event;

import java.time.LocalDateTime;

public record LoanReturnedEvent(Integer loanId, Integer memberId, Integer bookId, Integer copyId,
        LocalDateTime dueAt, LocalDateTime returnedAt, boolean damaged) {
}
