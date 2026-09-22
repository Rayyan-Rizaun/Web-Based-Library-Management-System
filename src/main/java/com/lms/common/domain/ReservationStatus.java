package com.lms.common.domain;

/**
 * {@code Reservation.Status} — CK_Reservation_Status.
 *
 * <p>Constant names are the stored values, so the column is mapped with
 * {@code @Enumerated(EnumType.STRING)}.
 */
public enum ReservationStatus {
    Waiting, Ready, Fulfilled, Cancelled, Expired
}
