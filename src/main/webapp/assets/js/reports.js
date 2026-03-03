document.addEventListener("DOMContentLoaded", () => {
  const todayLabel = document.getElementById("reportDate");
  if (todayLabel) {
    todayLabel.textContent = new Date().toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "2-digit"
    });
  }

  // Set default date range (last 30 days)
  const today = new Date();
  const prior = new Date();
  prior.setDate(prior.getDate() - 30);

  const fromEl = document.getElementById("dateFrom");
  const toEl   = document.getElementById("dateTo");
  if (fromEl) fromEl.value = prior.toISOString().split("T")[0];
  if (toEl)   toEl.value   = today.toISOString().split("T")[0];

  loadReport();
});

const BASE = (() => {
  const parts = window.location.pathname.split("/").filter(Boolean);
  return parts.length > 1 ? ("/" + parts[0]) : "";
})();

let mainChartInstance = null;
let roomChartInstance = null;

// ─── Entry Point ────────────────────────────────────────────────────────────

async function loadReport() {
  const type = document.getElementById("reportType")?.value || "reservations";
  const from = document.getElementById("dateFrom")?.value || "";
  const to   = document.getElementById("dateTo")?.value   || "";

  await Promise.allSettled([
    loadReportStats(from, to),
    loadReportCharts(from, to),
    loadReportTable(type, from, to)
  ]);
}

// ─── Stats ───────────────────────────────────────────────────────────────────

async function loadReportStats(from, to) {
  const statTotal     = document.getElementById("statTotal");
  const statRevenue   = document.getElementById("statRevenue");
  const statOccupancy = document.getElementById("statOccupancy");
  const statGuests    = document.getElementById("statGuests");

  try {
    const res = await fetch(`${BASE}/api/reservations/stats?from=${from}&to=${to}`, {
      credentials: "include"
    });
    if (!res.ok) throw new Error("Stats request failed: " + res.status);

    const data = await res.json();
    if (!data.success) throw new Error(data.message || "Stats failed");

    if (statTotal)     statTotal.textContent     = String(data.totalReservations || 0);
    if (statRevenue)   statRevenue.textContent   = formatMoney(Number(data.revenueThisMonth || 0));
    if (statOccupancy) statOccupancy.textContent = (data.occupancyRate || "0") + "%";
    if (statGuests)    statGuests.textContent    = String(data.uniqueGuests || 0);

  } catch (e) {
    if (statTotal)     statTotal.textContent     = "-";
    if (statRevenue)   statRevenue.textContent   = "-";
    if (statOccupancy) statOccupancy.textContent = "-";
    if (statGuests)    statGuests.textContent    = "-";
  }
}

// ─── Charts ──────────────────────────────────────────────────────────────────

async function loadReportCharts(from, to) {
  try {
    const res = await fetch(`${BASE}/api/reservations?from=${from}&to=${to}`, {
      credentials: "include"
    });
    if (!res.ok) throw new Error("Chart data request failed: " + res.status);

    const data = await res.json();
    if (!data.success) throw new Error(data.message || "Chart load failed");

    const reservations = data.reservations || [];

    // Monthly counts
    const monthly = {};
    for (const r of reservations) {
      if (!r.checkInDate) continue;
      const month = new Date(r.checkInDate).toLocaleDateString("en-US", { month: "short" });
      monthly[month] = (monthly[month] || 0) + 1;
    }

    // Room type breakdown
    const roomTypes = {};
    for (const r of reservations) {
      const rtype = r.roomType || "Unknown";
      roomTypes[rtype] = (roomTypes[rtype] || 0) + 1;
    }

    renderMainChart(Object.keys(monthly), Object.values(monthly));
    renderRoomChart(Object.keys(roomTypes), Object.values(roomTypes));

  } catch (e) {
    renderMainChart([], []);
    renderRoomChart([], []);
  }
}

function renderMainChart(labels, values) {
  const el = document.getElementById("mainReportChart");
  if (!el) return;

  if (mainChartInstance) mainChartInstance.destroy();

  if (!labels.length) {
    labels = ["No data"];
    values = [0];
  }

  mainChartInstance = new Chart(el, {
    type: "bar",
    data: {
      labels,
      datasets: [{
        label: "Reservations",
        data: values,
        backgroundColor: "rgba(13,110,253,0.7)",
        borderRadius: 6
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

function renderRoomChart(labels, values) {
  const el = document.getElementById("roomTypeChart");
  if (!el) return;

  if (roomChartInstance) roomChartInstance.destroy();

  const hasData = values.reduce((a, b) => a + b, 0) > 0;

  roomChartInstance = new Chart(el, {
    type: "doughnut",
    data: {
      labels: hasData ? labels : ["No data"],
      datasets: [{
        data: hasData ? values : [1],
        backgroundColor: ["#0d6efd", "#198754", "#ffc107", "#0dcaf0", "#dc3545"]
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false
    }
  });
}

// ─── Table ───────────────────────────────────────────────────────────────────

async function loadReportTable(type, from, to) {
  const tbody     = document.getElementById("reportTableBody");
  const rowCount  = document.getElementById("reportRowCount");
  if (!tbody) return;

  try {
    const res = await fetch(`${BASE}/api/reservations?from=${from}&to=${to}`, {
      credentials: "include"
    });
    if (!res.ok) throw new Error("Table request failed: " + res.status);

    const data = await res.json();
    if (!data.success) throw new Error(data.message || "Table load failed");

    const list = data.reservations || [];

    if (rowCount) rowCount.textContent = `${list.length} records`;

    if (!list.length) {
      tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">No records found for selected range.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map(r => {
      const id      = escapeHtml(r.reservationNumber || r.reservationId || "-");
      const guest   = escapeHtml(r.guestName || "-");
      const room    = escapeHtml((r.roomNumber ? r.roomNumber + " " : "") + (r.roomType || ""));
      const checkIn = escapeHtml(formatDateShort(r.checkInDate));
      const checkOut= escapeHtml(formatDateShort(r.checkOutDate));
      const status  = escapeHtml(r.status || "-");
      const amount  = formatMoney(Number(r.totalAmount || r.amount || 0));

      return `
        <tr>
          <td>${id}</td>
          <td>${guest}</td>
          <td>${room}</td>
          <td>${checkIn}</td>
          <td>${checkOut}</td>
          <td>${renderStatusBadge(status)}</td>
          <td class="text-end">${amount}</td>
        </tr>
      `;
    }).join("");

  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">Cannot load report data.</td></tr>`;
    if (rowCount) rowCount.textContent = "0 records";
  }
}

// ─── Export ──────────────────────────────────────────────────────────────────

function exportReport() {
  const type = document.getElementById("reportType")?.value || "reservations";
  const from = document.getElementById("dateFrom")?.value  || "";
  const to   = document.getElementById("dateTo")?.value    || "";
  window.location.href = `${BASE}/api/reports/export?type=${type}&from=${from}&to=${to}`;
}

// ─── Helpers (same as dashboard) ─────────────────────────────────────────────

function renderStatusBadge(status) {
  const s = String(status || "").toUpperCase();
  let cls = "bg-secondary";
  if (s.includes("CONFIRM") || s.includes("BOOK")) cls = "bg-success";
  else if (s.includes("PEND"))   cls = "bg-warning text-dark";
  else if (s.includes("CHECK"))  cls = "bg-info";
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
