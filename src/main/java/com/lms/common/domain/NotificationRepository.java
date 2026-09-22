package com.lms.common.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Aggregate root {@link Notification}. The top-bar bell and reminders sent by UC-03, 04, 06 and 10.
 */
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findTop20ByUserUserIdOrderByCreatedAtDesc(Integer userId);

    /** Unread badge count on the bell. */
    long countByUserUserIdAndReadFalse(Integer userId);

    List<Notification> findByUserUserIdAndReadFalseOrderByCreatedAtDesc(Integer userId);
}
