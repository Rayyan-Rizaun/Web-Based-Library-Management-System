// reservationController.js — HTTP handlers wrapping reservationService.

const service = require('./reservationService');

function handleError(res, err) {
  if (err instanceof service.ReservationError) {
    return res.status(err.statusCode).json({ success: false, message: err.message });
  }
  console.error(err);
  return res.status(500).json({ success: false, message: 'Internal server error.' });
}

// POST /api/reservations  { memberId, bookId }
async function reserveBook(req, res) {
  const { memberId, bookId } = req.body;
  if (!memberId || !bookId) {
    return res.status(400).json({ success: false, message: 'memberId and bookId are required.' });
  }
  try {
    const result = await service.createReservation(memberId, bookId);
    res.status(201).json({ success: true, data: result });
  } catch (err) {
    handleError(res, err);
  }
}

// GET /api/reservations/member/:memberId
async function getMemberReservations(req, res) {
  try {
    const rows = await service.getReservationsForMember(req.params.memberId);
    res.json({ success: true, data: rows });
  } catch (err) {
    handleError(res, err);
  }
}

// GET /api/reservations/book/:bookId/waitlist   (Librarian)
async function getBookWaitingList(req, res) {
  try {
    const rows = await service.getWaitingList(req.params.bookId);
    res.json({ success: true, data: rows });
  } catch (err) {
    handleError(res, err);
  }
}

// DELETE /api/reservations/:id   body: { memberId }  (Member cancels own)
async function cancelAsMember(req, res) {
  try {
    const result = await service.cancelReservation(req.params.id, {
      memberId: Number(req.body.memberId),
      actorRole: 'Member',
    });
    res.json({ success: true, data: result });
  } catch (err) {
    handleError(res, err);
  }
}

// DELETE /api/reservations/:id/librarian   (Librarian cancels any)
async function cancelAsLibrarian(req, res) {
  try {
    const result = await service.cancelReservation(req.params.id, { actorRole: 'Librarian' });
    res.json({ success: true, data: result });
  } catch (err) {
    handleError(res, err);
  }
}

// POST /api/reservations/:id/fulfill   (Librarian, at issue counter)
async function fulfill(req, res) {
  try {
    const result = await service.fulfillReservation(req.params.id);
    res.json({ success: true, data: result });
  } catch (err) {
    handleError(res, err);
  }
}

// POST /api/reservations/book/:bookId/notify-next
// Called by the Borrowing & Return module when a copy is returned.
async function notifyNext(req, res) {
  try {
    const reservationId = await service.notifyNextInQueue(req.params.bookId);
    res.json({ success: true, data: { notifiedReservationId: reservationId } });
  } catch (err) {
    handleError(res, err);
  }
}

// POST /api/reservations/expire-holds  (cron / admin trigger)
async function expireHolds(req, res) {
  try {
    const count = await service.expireOverdueHolds();
    res.json({ success: true, data: { expiredCount: count } });
  } catch (err) {
    handleError(res, err);
  }
}

module.exports = {
  reserveBook,
  getMemberReservations,
  getBookWaitingList,
  cancelAsMember,
  cancelAsLibrarian,
  fulfill,
  notifyNext,
  expireHolds,
};
