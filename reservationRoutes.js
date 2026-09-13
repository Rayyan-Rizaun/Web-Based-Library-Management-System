// reservationRoutes.js
const express = require('express');
const router = express.Router();
const ctrl = require('./reservationController');

// Member-facing (PB-07)
router.post('/', ctrl.reserveBook);                                 // reserve a book
router.get('/member/:memberId', ctrl.getMemberReservations);        // view my reservations/status
router.delete('/:id', ctrl.cancelAsMember);                         // cancel my reservation

// Librarian-facing (PB-12)
router.get('/book/:bookId/waitlist', ctrl.getBookWaitingList);      // view waiting list for a book
router.delete('/:id/librarian', ctrl.cancelAsLibrarian);            // cancel any reservation
router.post('/:id/fulfill', ctrl.fulfill);                          // mark collected at issue desk

// System / integration hooks
router.post('/book/:bookId/notify-next', ctrl.notifyNext);          // call after a book is returned
router.post('/expire-holds', ctrl.expireHolds);                     // run periodically (cron)

module.exports = router;
