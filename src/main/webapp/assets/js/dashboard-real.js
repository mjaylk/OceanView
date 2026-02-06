document.addEventListener("DOMContentLoaded", () => {
  // Basic date label
  const todayLabel = document.getElementById("todayLabel");
  if (todayLabel) {
    todayLabel.textContent = new Date().toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "2-digit"
    });
  }

  // Sidebar
  window.toggleSidebar = () => {
    const sb = document.querySelector(".sidebar");
    if (sb) sb.classList.toggle("open");
  };

  loadDashboard();
});


const BASE = (() => {
  const parts = window.location.pathname.split("/").filter(Boolean);
  return parts.length > 1 ? ("/" + parts[0]) : "";
})();

let resChartInstance = null;
let occChartInstance = null;

async function loadDashboard() {
  await Promise.allSettled([
    loadGuestsCount(),
    loadRoomsStats(),
    loadReservationStatsAndCharts(),
    loadRecentReservations()
  ]);
}

async function loadGuestsCount() {
  const el = document.getElementById("statGuests");
  const note = document.getElementById("statGuestNote");

  try {
    const res = await fetch(`${BASE}/api/guests`, { credentials: "include" });
    if (!res.ok) throw new Error("Guests request failed: " + res.status);

    const data = await res.json();
    if (!data.success) throw new Error(data.message || "Guests load failed");

    const guests = data.guests || [];
    el.textContent = String(guests.length);
    note.textContent = "Based on guest records";
  } catch (e) {
    el.textContent = "-";
    note.textContent = "Cannot load";
  }
}

async function loadRoomsStats() {
  const el = document.getElementById("statRooms");
  const note = document.getElementById("statRoomNote");

  try {
    const res = await fetch(`${BASE}/api/rooms`, { credentials: "include" });
    if (!res.ok) throw new Error("Rooms request failed: " + res.status);

    const data = await res.json();
    if (!data.success) throw new Error(data.message || "Rooms load failed");

    const rooms = data.rooms || [];
    const total = rooms.length;

    const occupied = rooms.filter(r => String(r.status).toUpperCase() === "OCCUPIED").length;
    el.textContent = `${occupied}/${total}`;
    note.textContent = `${Math.max(total - occupied, 0)} available`;

    renderOccChart(occupied, Math.max(total - occupied, 0));
  } catch (e) {
    el.textContent = "-/ -";
    note.textContent = "Cannot load";
    renderOccChart(0, 0);
  }
}

async function loadReservationStatsAndCharts() {
  const statEl = document.getElementById("statReservations");
  const statNote = document.getElementById("statResNote");
  const revEl = document.getElementById("statRevenue");
  const revNote = document.getElementById("statRevenueNote");

  try {
    const url = `${BASE}/api/reservations/stats?days=30`;
    const res = await fetch(url, { credentials: "include" });
    if (!res.ok) throw new Error("Stats request failed: " + res.status);

    const data = await res.json();
    if (!data.success) throw new Error(data.message || "Reservation stats failed");

    const totalReservations = Number(data.totalReservations || 0);
    statEl.textContent = String(totalReservations);
    statNote.textContent = "Last 30 days";

    const revenue = Number(data.revenueThisMonth || 0);
    revEl.textContent = formatMoney(revenue);
    revNote.textContent = "This month";

    const series = Array.isArray(data.series) ? data.series : [];
    const labels = series.map(x => x.label);
    const counts = series.map(x => Number(x.count || 0));

    renderResChart(labels, counts);
  } catch (e) {
    statEl.textContent = "-";
    statNote.textContent = "";
    revEl.textContent = "-";
    revNote.textContent = "Cannot load";
    renderResChart([], []);
  }
}

async function loadRecentReservations() {
  const tbody = document.getElementById("recentResTbody");
  if (!tbody) return;

  try {
   
    const res = await fetch(`${BASE}/api/reservations?limit=8`, { credentials: "include" });
    if (!res.ok) throw new Error("Recent reservations request failed: " + res.status);

    const data = await res.json();
    if (!data.success) throw new Error(data.message || "Recent reservations failed");

    const list = data.reservations || [];
    if (!list.length) {
      tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">No reservations found.</td></tr>`;
      return;
    }

    const limited = list.slice(0, 8);

    tbody.innerHTML = limited.map(r => {
      const id = escapeHtml(r.reservationNumber || r.reservationId || "-");
      const guest = escapeHtml(r.guestName || "-");
      const room = escapeHtml((r.roomNumber ? r.roomNumber + " " : "") + (r.roomType || ""));
      const checkIn = escapeHtml(formatDateShort(r.checkInDate));
      const status = escapeHtml(r.status || "-");
      const amount = formatMoney(Number(r.totalAmount || r.amount || 0));

      return `
        <tr>
          <td>${id}</td>
          <td>${guest}</td>
          <td>${room}</td>
          <td>${checkIn}</td>
          <td>${renderStatusBadge(status)}</td>
          <td class="text-end">${amount}</td>
        </tr>
      `;
    }).join("");
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">Cannot load recent reservations.</td></tr>`;
  }
}

function renderResChart(labels, dataPoints) {
  const el = document.getElementById("resChart");
  if (!el) return;

  if (resChartInstance) resChartInstance.destroy();

  if (!labels.length) {
    labels = ["No data"];
    dataPoints = [0];
  }

  resChartInstance = new Chart(el, {
    type: "line",
    data: {
      labels,
      datasets: [{
        label: "Reservations",
        data: dataPoints,
        tension: 0.35,
        fill: true
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: { y: { beginAtZero: true } }
    }
  });
}

function renderOccChart(occupied, available) {
  const el = document.getElementById("occChart");
  if (!el) return;

  if (occChartInstance) occChartInstance.destroy();

  const hasData = (occupied + available) > 0;

  occChartInstance = new Chart(el, {
    type: "doughnut",
    data: {
      labels: ["Occupied", "Available"],
      datasets: [{
        data: hasData ? [occupied, available] : [0, 0]
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false
    }
  });
}

function renderStatusBadge(status) {
  const s = String(status || "").toUpperCase();
  let cls = "bg-secondary";

  if (s.includes("CONFIRM")) cls = "bg-success";
  else if (s.includes("PEND")) cls = "bg-warning text-dark";
  else if (s.includes("CHECK")) cls = "bg-info";
  else if (s.includes("CANCEL")) cls = "bg-danger";

  return `<span class="badge ${cls}">${escapeHtml(status)}</span>`;
}

function formatMoney(n) {
  const num = Number.isFinite(n) ? n : 0;
  return "$" + num.toFixed(2);
}

function formatDateShort(iso) {
  if (!iso) return "-";
  const d = new Date(iso);
  if (isNaN(d.getTime())) return iso;
  return d.toLocaleDateString("en-US", { month: "short", day: "2-digit" });
}

function escapeHtml(s) {
  return String(s || "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
