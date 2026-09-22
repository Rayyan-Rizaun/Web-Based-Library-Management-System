package com.lms.common.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table {@code Reservation} — entity RESERVATION: 1:N PLACES from
 * {@link Member} and 1:N RESERVES from {@link Book} (R8). The queue is per
 * title, not per copy (business-rules §3).
 *
 * <p>No QueuePosition field (R12): a waiting reservation's position is one
 * more than the number of Waiting reservations for the same book requested
 * before it — {@code ReservationRepository.countByBookBookIdAndStatusAndRequestedAtBefore}.
 */
@Entity
@Table(name = "Reservation")
@Getter
@Setter
@NoArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReservationID")
    @Setter(AccessLevel.NONE)
    private Integer reservationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BookID", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MemberID", nullable = false)
    private Member member;

    @Column(name = "RequestedAt", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 10)
    private ReservationStatus status = ReservationStatus.Waiting;

    /** When a copy was set aside. Set together with {@link #expiresAt}. */
    @Column(name = "ReadyAt")
    private LocalDateTime readyAt;

    /** ReadyAt + the 'Reservation.HoldDays' setting. */
    @Column(name = "ExpiresAt")
    private LocalDateTime expiresAt;

    /** Set when the status becomes Fulfilled, Cancelled or Expired. */
    @Column(name = "ClosedAt")
    private LocalDateTime closedAt;

    @PrePersist
    void onCreate() {
        if (requestedAt == null) {
            requestedAt = DbTime.now();
        }
    }
}
