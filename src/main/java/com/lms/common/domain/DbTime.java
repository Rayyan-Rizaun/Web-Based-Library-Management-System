package com.lms.common.domain;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Timestamps for entity lifecycle callbacks. */
final class DbTime {

    private DbTime() {
    }

    /**
     * Current time truncated to whole seconds. Every timestamp column is
     * DATETIME2(0), which stores no fraction of a second; truncating here
     * keeps the value in memory identical to the stored value, so checks such
     * as {@code UpdatedAt >= CreatedAt} hold on both sides.
     */
    static LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
