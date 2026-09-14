// Backend base URL — change this if your Spring Boot app runs on a different host/port.
const BACKEND_BASE_URL = 'http://localhost:8080';
const API = `${BACKEND_BASE_URL}/api/reservations`;

function showMessage(text, ok = true) {
  const el = document.getElementById('msg');
  el.textContent = text;
  el.style.color = ok ? '#1e7a43' : '#a12525';
}

async function reserveBook() {
  const memberId = document.getElementById('memberId').value;
  const bookId = document.getElementById('bookId').value;
  if (!memberId || !bookId) {
    showMessage('Please enter both Member ID and Book ID.', false);
    return;
  }

  try {
    const res = await fetch(API, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ memberId: Number(memberId), bookId: Number(bookId) }),
    });
    const data = await res.json();
    if (!data.success) {
      showMessage(data.message, false);
      return;
    }
    showMessage(`Reserved! You are position ${data.data.queuePosition} in the queue.`);
    loadReservations();
  } catch (err) {
    showMessage('Could not reach the server.', false);
  }
}

async function loadReservations() {
  const memberId = document.getElementById('memberId').value;
  const tbody = document.getElementById('resBody');
  if (!memberId) {
    showMessage('Enter your Member ID first.', false);
    return;
  }

  const res = await fetch(`${API}/member/${memberId}`);
  const data = await res.json();
  if (!data.success) {
    showMessage(data.message, false);
    return;
  }

  if (data.data.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6">No reservations found.</td></tr>';
    return;
  }

  tbody.innerHTML = data.data.map(r => `
    <tr>
      <td>${r.bookTitle}</td>
      <td>${r.queuePosition ?? '-'}</td>
      <td><span class="status ${r.status}">${r.status}</span></td>
      <td>${new Date(r.reservationDate).toLocaleDateString()}</td>
      <td>${r.holdExpiryDate ? new Date(r.holdExpiryDate).toLocaleString() : '-'}</td>
      <td>
        ${['Pending', 'Available'].includes(r.status)
          ? `<button class="cancel" onclick="cancelReservation(${r.reservationId})">Cancel</button>`
          : ''}
      </td>
    </tr>
  `).join('');
}

async function cancelReservation(reservationId) {
  const memberId = document.getElementById('memberId').value;
  const res = await fetch(`${API}/${reservationId}`, {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ memberId: Number(memberId) }),
  });
  const data = await res.json();
  showMessage(data.success ? 'Reservation cancelled.' : data.message, data.success);
  loadReservations();
}
