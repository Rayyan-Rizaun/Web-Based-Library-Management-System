// server.js — minimal Express app so this module can run standalone.
// In the full system, mount reservationRoutes on your main app instead.

const express = require('express');
const path = require('path');
const reservationRoutes = require('./reservationRoutes');

const app = express();
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

app.use('/api/reservations', reservationRoutes);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Reservation Management module running on http://localhost:${PORT}`);
  console.log('Member page:    /member-reservations.html');
  console.log('Librarian page: /librarian-reservations.html');
});
