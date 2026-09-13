// reservationService.js
// Business logic for Reservation Management (PB-07, PB-12).
//
// Rules implemented:
//  - A member can only reserve a book that currently has 0 available copies
//    (per Figure 4: "if a book is currently borrowed by another member,
//    the member should place a reservation online").
//  - Reservations are served fairly, first-come-first-served (queue_position).
//  - When a copy is returned, the next person in line is notified and given
//    a hold window (default 48 hours) to collect the book before it expires
//    and moves to the next person.
//  - Members can cancel their own pending reservation; librarians can cancel
//    or fulfill any reservation.

const pool = require('./db');

const HOLD_WINDOW_HOURS = 48;

class ReservationError extends Error {
  constructor(message, statusCode = 400) {
    super(message);
    this.statusCode = statusCode;
  }
}

/**
 * Member places a reservation on a book that is currently unavailable.
 */
async function createReservation(memberId, bookId) {
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();

    const [[book]] = await conn.query(
      'SELECT book_id, available_copies FROM books WHERE book_id = ? FOR UPDATE',
      [bookId]
    );
    if (!book) throw new ReservationError('Book not found.', 404);

    if (book.available_copies > 0) {
      throw new ReservationError(
        'This book currently has copies available — please borrow it directly instead of reserving.'
      );
    }

    const [[existing]] = await conn.query(
      `SELECT reservation_id FROM reservations
       WHERE member_id = ? AND book_id = ? AND status IN ('Pending','Available')`,
      [memberId, bookId]
    );
    if (existing) {
      throw new ReservationError('You already have an active reservation for this book.');
    }

    const [[{ nextPos }]] = await conn.query(
      `SELECT COALESCE(MAX(queue_position), 0) + 1 AS nextPos
       FROM reservations WHERE book_id = ? AND status IN ('Pending','Available')`,
      [bookId]
    );

    const [result] = await conn.query(
      `INSERT INTO reservations (member_id, book_id, queue_position, status)
       VALUES (?, ?, ?, 'Pending')`,
      [memberId, bookId, nextPos]
    );

    await conn.commit();
    return { reservationId: result.insertId, queuePosition: nextPos, status: 'Pending' };
  } catch (err) {
    await conn.rollback();
    throw err;
  } finally {
    conn.release();
  }
}

/**
 * Member views their own reservation history and live status.
 */
async function getReservationsForMember(memberId) {
  const [rows] = await pool.query(
    `SELECT r.reservation_id, r.book_id, b.title, r.reservation_date, r.queue_position,
            r.status, r.notified_date, r.hold_expiry_date
     FROM reservations r
     JOIN books b ON b.book_id = r.book_id
     WHERE r.member_id = ?
     ORDER BY r.reservation_date DESC`,
    [memberId]
  );
  return rows;
}

/**
 * Librarian view: full waiting list for one book, in fair (FIFO) order.
 */
async function getWaitingList(bookId) {
  const [rows] = await pool.query(
    `SELECT r.reservation_id, r.member_id, m.full_name, r.queue_position,
            r.status, r.reservation_date, r.notified_date, r.hold_expiry_date
     FROM reservations r
     JOIN members m ON m.member_id = r.member_id
     WHERE r.book_id = ? AND r.status IN ('Pending','Available')
     ORDER BY r.queue_position ASC`,
    [bookId]
  );
  return rows;
}

/**
 * Cancel a reservation. Members may only cancel their own; librarians may
 * cancel any (pass actorRole = 'Librarian' and omit memberId check).
 */
async function cancelReservation(reservationId, { memberId = null, actorRole = 'Member' }) {
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();

    const [[res]] = await conn.query(
      'SELECT * FROM reservations WHERE reservation_id = ? FOR UPDATE',
      [reservationId]
    );
    if (!res) throw new ReservationError('Reservation not found.', 404);
    if (actorRole === 'Member' && res.member_id !== memberId) {
      throw new ReservationError('You can only cancel your own reservation.', 403);
    }
    if (!['Pending', 'Available'].includes(res.status)) {
      throw new ReservationError('Only pending or held reservations can be cancelled.');
    }

    await conn.query(
      `UPDATE reservations SET status = 'Cancelled', cancelled_by = ? WHERE reservation_id = ?`,
      [actorRole, reservationId]
    );

    // Close the gap in the queue for everyone behind this reservation.
    await conn.query(
      `UPDATE reservations
       SET queue_position = queue_position - 1
       WHERE book_id = ? AND status IN ('Pending','Available') AND queue_position > ?`,
      [res.book_id, res.queue_position]
    );

    // If the cancelled reservation was the one currently "Available" (holding
    // a copy), release that copy to the next person in line.
    if (res.status === 'Available') {
      await conn.commit();
      await notifyNextInQueue(res.book_id);
      return { reservationId, status: 'Cancelled' };
    }

    await conn.commit();
    return { reservationId, status: 'Cancelled' };
  } catch (err) {
    await conn.rollback();
    throw err;
  } finally {
    conn.release();
  }
}

/**
 * Called by the Borrowing & Return module whenever a copy of a book becomes
 * available (a return is processed, or a held reservation is fulfilled/expired).
 * Moves the earliest Pending reservation into 'Available' status and starts
 * the hold window.
 */
async function notifyNextInQueue(bookId) {
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();

    const [[next]] = await conn.query(
      `SELECT reservation_id, member_id FROM reservations
       WHERE book_id = ? AND status = 'Pending'
       ORDER BY queue_position ASC LIMIT 1 FOR UPDATE`,
      [bookId]
    );

    if (!next) {
      await conn.commit();
      return null; // no one waiting
    }

    await conn.query(
      `UPDATE reservations
       SET status = 'Available',
           notified_date = NOW(),
           hold_expiry_date = DATE_ADD(NOW(), INTERVAL ? HOUR)
       WHERE reservation_id = ?`,
      [HOLD_WINDOW_HOURS, next.reservation_id]
    );

    await conn.commit();

    // Stub: plug in the real notification channel (email/SMS/dashboard alert).
    console.log(
      `[Notification] Member ${next.member_id}: your reserved book is now available. ` +
      `Please collect it within ${HOLD_WINDOW_HOURS} hours.`
    );

    return next.reservation_id;
  } catch (err) {
    await conn.rollback();
    throw err;
  } finally {
    conn.release();
  }
}

/**
 * Librarian marks a held reservation as fulfilled once the member actually
 * borrows the book at the counter (called from the Borrowing module).
 */
async function fulfillReservation(reservationId) {
  const [[res]] = await pool.query('SELECT * FROM reservations WHERE reservation_id = ?', [reservationId]);
  if (!res) throw new ReservationError('Reservation not found.', 404);
  if (res.status !== 'Available') {
    throw new ReservationError('Reservation is not currently on hold for collection.');
  }
  await pool.query(`UPDATE reservations SET status = 'Fulfilled' WHERE reservation_id = ?`, [reservationId]);
  return { reservationId, status: 'Fulfilled' };
}

/**
 * Scheduled job (e.g. run hourly via cron) — expires holds whose deadline has
 * passed and automatically passes the copy to the next person in the queue.
 */
async function expireOverdueHolds() {
  const [expired] = await pool.query(
    `SELECT reservation_id, book_id FROM reservations
     WHERE status = 'Available' AND hold_expiry_date < NOW()`
  );

  for (const r of expired) {
    await pool.query(
      `UPDATE reservations SET status = 'Expired', cancelled_by = 'System' WHERE reservation_id = ?`,
      [r.reservation_id]
    );
    await pool.query(
      `UPDATE reservations
       SET queue_position = queue_position - 1
       WHERE book_id = ? AND status IN ('Pending','Available')
         AND queue_position > (SELECT queue_position FROM (
             SELECT queue_position FROM reservations WHERE reservation_id = ?) t)`,
      [r.book_id, r.reservation_id]
    );
    await notifyNextInQueue(r.book_id);
  }
  return expired.length;
}

module.exports = {
  ReservationError,
  createReservation,
  getReservationsForMember,
  getWaitingList,
  cancelReservation,
  notifyNextInQueue,
  fulfillReservation,
  expireOverdueHolds,
};
