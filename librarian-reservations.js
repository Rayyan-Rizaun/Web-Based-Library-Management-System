// Backend base URL — change this if your Spring Boot app runs on a different host/port.
const BACKEND_BASE_URL = 'http://localhost:8080';
const API = `${BACKEND_BASE_URL}/api/reservations`;

function showMessage(text, ok = true) {
  const el = document.getElementById('msg');
  el.textContent = text;
  el.style.color = ok ? '#1e7a43' : '#a12525';
}

async function loadWaitlist() {
  const bookId = document.getElementById('bookId').value;
  const tbody = document.getElementById('waitlistBody');
  if (!bookId) {
    showMessage('Enter a Book ID first.', false);
    return;
  }

  const res = await fetch(`${API}/book/${bookId}/waitlist`);
  const data = await res.json();
  if (!data.success) {
    showMessage(data.message, false);
    return;
  }

  if (data.data.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6">No one is currently waiting for this book.</td></tr>';
    return;
  }

  tbody.innerHTML = data.data.map(r => `
    <tr>
      <td>${r.queuePosition}</td>
      <td>${r.memberName} (ID ${r.memberId})</td>
      <td><span class="status ${r.status}">${r.status}</span></td>
      <td>${new Date(r.reservationDate).toLocaleDateString()}</td>
      <td>${r.holdExpiryDate ? new Date(r.holdExpiryDate).toLocaleString() : '-'}</td>
      <td>
        ${r.status === 'Available'
          ? `<button class="success" onclick="fulfill(${r.reservationId})">Mark Collected</button>`
          : ''}
        <button class="danger" onclick="cancelReservation(${r.reservationId})">Cancel</button>
      </td>
    </tr>
  `).join('');
}

async function notifyNext() {
  const bookId = document.getElementById('bookId').value;
  const res = await fetch(`${API}/book/${bookId}/notify-next`, { method: 'POST' });
  const data = await res.json();
  if (data.success && data.data.notifiedReservationId !== -1) {
    showMessage(`Reservation #${data.data.notifiedReservationId} notified and given a hold.`);
  } else {
    showMessage('No one is waiting, or the copy is still unavailable.', false);
  }
  loadWaitlist();
}

async function fulfill(reservationId) {
  const res = await fetch(`${API}/${reservationId}/fulfill`, { method: 'POST' });
  const data = await res.json();
  showMessage(data.success ? 'Reservation marked as collected/fulfilled.' : data.message, data.success);
  loadWaitlist();
}

async function cancelReservation(reservationId) {
  const res = await fetch(`${API}/${reservationId}/librarian`, { method: 'DELETE' });
  const data = await res.json();
  showMessage(data.success ? 'Reservation cancelled.' : data.message, data.success);
  loadWaitlist();
}
