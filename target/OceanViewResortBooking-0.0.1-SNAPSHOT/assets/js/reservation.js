
const BASE = "/OceanViewResortBooking";
const tbody = document.querySelector("#resTable tbody");
const roomsById = new Map();

let guestSearchTimer = null;
let lastGuestResults = [];
let bookedRanges = [];
let reservationsCache = [];
let calendar = null;

document.addEventListener("DOMContentLoaded", () => {
  const todayLabel = document.getElementById("todayLabel");
  if (todayLabel) todayLabel.textContent = new Date().toLocaleDateString();

  bindPricingListeners();
  wireModalPricingRebind();
  wireSafeFieldListeners();

  loadReservations();
  initCalendarIfNeeded();
});

function showAlert(scope, message, level = "danger") {
  const wrap = document.getElementById(scope + "AlertWrap");
  const alert = document.getElementById(scope + "Alert");
  const msg = document.getElementById(scope + "AlertMsg");
  if (!wrap || !alert || !msg) return;

  wrap.style.display = "block";
  alert.className = `alert alert-${level} alert-dismissible fade show`;
  msg.textContent = message;
}

function hideAlert(scope) {
  const wrap = document.getElementById(scope + "AlertWrap");
  if (wrap) wrap.style.display = "none";
}

function setSaving(isSaving) {
  const btn = document.getElementById("btnSave");
  const sp = document.getElementById("btnSaveSpinner");
  if (btn) btn.disabled = isSaving;
  if (sp) sp.style.display = isSaving ? "inline-block" : "none";
}

function todayIso() { return new Date().toISOString().slice(0, 10); }
function money(v) { const n = Number(v || 0); return "$" + n.toFixed(2); }

function escapeHtml(s) {
  return String(s)
    .replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;").replaceAll("'", "&#039;");
}

function setStatusBadge(el, status) {
  if (!el) return;

  const s = String(status || "").toUpperCase();
  el.textContent = s || "-";
  el.className = "badge " + (
    s === "CONFIRMED" ? "text-bg-success" :
    s === "PENDING" ? "text-bg-warning" :
    s === "CHECKED_IN" ? "text-bg-info" :
    "text-bg-secondary"
  );
}

function openNewReservationModal() {
  resetReservationForm();
  document.getElementById("reservationModalTitle").innerHTML =
    `<i class="fas fa-calendar-plus me-2 text-primary"></i>New Reservation`;
  loadRoomsDropdown();
  initGuestSearch();
  lockPastDates();
  bindPricingListeners();
}

function resetReservationForm() {
  const form = document.getElementById("reservationForm");
  if (form) form.reset();

  document.getElementById("reservationId").value = "";
  document.getElementById("guestId").value = "";
  document.getElementById("calcTax").value = "0";
  document.getElementById("calcDiscount").value = "0";
  document.getElementById("roomMeta").textContent = "";
  document.getElementById("roomBlockedHint").textContent = "";
  document.getElementById("guestSuggestions").innerHTML = "";
  bookedRanges = [];

  hideAlert("form");
  renderCalc({ nights: "", rate: "", subtotal: "", total: "" });
}

function lockPastDates() {
  const min = todayIso();
  const inEl = document.getElementById("checkIn");
  const outEl = document.getElementById("checkOut");
  if (inEl) inEl.min = min;
  if (outEl) outEl.min = min;
}

function parseIsoDate(s) {
  if (!s) return new Date(NaN);
  const [y, m, d] = s.split("-").map(Number);
  return new Date(y, m - 1, d);
}

function overlaps(startIso, endIso, range) {
  const s1 = parseIsoDate(startIso).getTime();
  const e1 = parseIsoDate(endIso).getTime();
  const s2 = parseIsoDate(range.checkInDate).getTime();
  const e2 = parseIsoDate(range.checkOutDate).getTime();
  return s1 < e2 && e1 > s2;
}

function validateAgainstBooked() {
  const roomId = Number(document.getElementById("roomId").value || 0);
  const inD = document.getElementById("checkIn").value;
  const outD = document.getElementById("checkOut").value;
  const editingId = Number(document.getElementById("reservationId").value || 0);

  if (!roomId || !inD || !outD) return true;

  if (outD <= inD) {
    showAlert("form", "Check-out date must be after check-in date.", "warning");
    return false;
  }

  const conflict = bookedRanges.some(r =>
    Number(r.reservationId) !== editingId &&
    overlaps(inD, outD, r)
  );

  if (conflict) {
    showAlert("form", "Selected dates overlap with an existing reservation for this room.", "danger");
    return false;
  }

  return true;
}

function getRoomRate(room) {
  const v = room?.ratePerNight ?? room?.rate_per_night ?? room?.price ?? room?.rate ?? 0;
  return Number(v || 0);
}

function calcNights(checkInIso, checkOutIso) {
  if (!checkInIso || !checkOutIso) return 0;
  const ms = parseIsoDate(checkOutIso).getTime() - parseIsoDate(checkInIso).getTime();
  const n = Math.floor(ms / (1000 * 60 * 60 * 24));
  return n > 0 ? n : 0;
}

function round2(n) { return Math.round((Number(n) + Number.EPSILON) * 100) / 100; }

function renderCalc({ nights, rate, subtotal, total }) {
  document.getElementById("calcNights").value = nights;
  document.getElementById("calcRate").value = rate === "" ? "" : money(rate);
  document.getElementById("calcSubtotal").value = subtotal === "" ? "" : money(subtotal);
  document.getElementById("calcTotal").value = total === "" ? "" : money(total);
}

function getTaxRateInput() {
  const v = parseFloat(document.getElementById("calcTax").value);
  return Number.isFinite(v) ? v : 0;
}

function getDiscountInput() {
  const v = parseFloat(document.getElementById("calcDiscount").value);
  return Number.isFinite(v) ? v : 0;
}

function recalcPricing() {
  const roomId = Number(document.getElementById("roomId").value || 0);
  const room = roomsById.get(String(roomId));
  const inD = document.getElementById("checkIn").value;
  const outD = document.getElementById("checkOut").value;

  if (!roomId || !room) {
    renderCalc({ nights: "", rate: "", subtotal: "", total: "" });
    return;
  }

  const rate = getRoomRate(room);
  const nights = calcNights(inD, outD);

  if (!inD || !outD || !nights || rate <= 0) {
    renderCalc({
      nights: nights ? nights : "",
      rate: rate > 0 ? rate : "",
      subtotal: "",
      total: ""
    });
    return;
  }

  const subtotal = round2(nights * rate);
  const taxRate = getTaxRateInput();
  const discount = getDiscountInput();

  const taxAmount = round2(subtotal * (taxRate / 100));
  let total = round2(subtotal + taxAmount - discount);
  if (total < 0) total = 0;

  renderCalc({ nights, rate, subtotal, total });
}

function bindPricingListeners() {
  const taxEl = document.getElementById("calcTax");
  const discEl = document.getElementById("calcDiscount");

  if (taxEl) {
    taxEl.oninput = recalcPricing;
    taxEl.onchange = recalcPricing;
    taxEl.onkeyup = recalcPricing;
  }
  if (discEl) {
    discEl.oninput = recalcPricing;
    discEl.onchange = recalcPricing;
    discEl.onkeyup = recalcPricing;
  }
}

function wireModalPricingRebind() {
  const modalEl = document.getElementById("reservationModal");
  if (!modalEl) return;

  modalEl.addEventListener("shown.bs.modal", () => {
    bindPricingListeners();
    recalcPricing();
  });
}

function wireSafeFieldListeners() {
  const inEl = document.getElementById("checkIn");
  const outEl = document.getElementById("checkOut");
  const roomEl = document.getElementById("roomId");

  if (inEl) {
    inEl.addEventListener("change", () => {
      hideAlert("form");
      const inD = inEl.value;
      if (inD && outEl) outEl.min = inD;
      validateAgainstBooked();
      recalcPricing();
    });
  }

  if (outEl) {
    outEl.addEventListener("change", () => {
      hideAlert("form");
      validateAgainstBooked();
      recalcPricing();
    });
  }

  if (roomEl) {
    roomEl.addEventListener("change", async () => {
      const roomId = Number(roomEl.value || 0);
      const room = roomsById.get(String(roomId));
      const meta = document.getElementById("roomMeta");

      if (!room) {
        if (meta) meta.textContent = "";
        recalcPricing();
        return;
      }

      const rate = getRoomRate(room);
      if (meta) meta.textContent = `Max guests: ${room.maxGuests ?? 2} | Status: ${room.status} | Rate: ${money(rate)}`;

      await loadBookedRangesForRoom(room.roomId);
      validateAgainstBooked();
      recalcPricing();
    });
  }
}

function initGuestSearch() {
  const input = document.getElementById("guestSearch");
  const dl = document.getElementById("guestSuggestions");
  if (!input || !dl) return;

  if (input.dataset.bound === "1") return;
  input.dataset.bound = "1";

  input.addEventListener("input", () => {
    clearTimeout(guestSearchTimer);
    guestSearchTimer = setTimeout(async () => {
      const q = input.value.trim();
      document.getElementById("guestId").value = "";

      if (q.length < 3) {
        dl.innerHTML = "";
        lastGuestResults = [];
        return;
      }

      try {
        const res = await fetch(`${BASE}/api/guests/search?q=${encodeURIComponent(q)}`, { credentials: "same-origin" });
        const txt = await res.text();
        let data = {};
        try { data = JSON.parse(txt); } catch (e) {}

        if (!res.ok) {
          dl.innerHTML = "";
          lastGuestResults = [];
          return;
        }

        const guests = data.guests || [];
        lastGuestResults = guests;

        dl.innerHTML = guests.map(g => {
          const label = `${g.fullName} | ${g.email || "-"} | ${g.contactNumber || "-"}`;
          return `<option value="${escapeHtml(label)}"></option>`;
        }).join("");

      } catch (e) {
        dl.innerHTML = "";
        lastGuestResults = [];
      }
    }, 350);
  });

  input.addEventListener("change", () => {
    const val = input.value.trim();
    const g = lastGuestResults.find(x => (`${x.fullName} | ${x.email || "-"} | ${x.contactNumber || "-"}`) === val);
    if (!g) return;

    document.getElementById("guestId").value = g.guestId;
    document.getElementById("guestName").value = g.fullName || "";
    document.getElementById("guestEmail").value = g.email || "";
    document.getElementById("guestContactNumber").value = g.contactNumber || "";
  });
}

async function loadRoomsDropdown() {
  const sel = document.getElementById("roomId");
  const meta = document.getElementById("roomMeta");
  if (!sel) return;

  sel.innerHTML = `<option value="">Loading rooms...</option>`;
  roomsById.clear();
  if (meta) meta.textContent = "";
  bookedRanges = [];
  const hint = document.getElementById("roomBlockedHint");
  if (hint) hint.textContent = "";

  const today = todayIso();
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const tomorrowIso = tomorrow.toISOString().slice(0, 10);

  try {
    const res = await fetch(`${BASE}/api/rooms/availability?checkIn=${today}&checkOut=${tomorrowIso}`, { credentials: "same-origin" });
    const txt = await res.text();
    let data = {};
    try { data = JSON.parse(txt); } catch (e) {}

    if (!res.ok) {
      showAlert("form", data.message || "Failed to load rooms.", "danger");
      sel.innerHTML = `<option value="">-- No rooms --</option>`;
      return;
    }

    const rooms = data.rooms || [];
    if (!rooms.length) {
      sel.innerHTML = `<option value="">-- No rooms available --</option>`;
      return;
    }

    sel.innerHTML = `<option value="">Select a room</option>`;
    rooms.forEach(r => {
      roomsById.set(String(r.roomId), r);
      const rate = getRoomRate(r);
      const statusLabel = r.status === "BOOKED" ? " (BOOKED)" : r.status === "MAINTENANCE" ? " (MAINTENANCE)" : "";
      sel.add(new Option(`${r.roomNumber} - ${r.roomType} - ${money(rate)}${statusLabel}`, r.roomId));
    });

    bindPricingListeners();
    recalcPricing();

  } catch (err) {
    showAlert("form", "Network error loading rooms: " + err.message, "danger");
    sel.innerHTML = `<option value="">-- Failed to load --</option>`;
  }
}

async function loadBookedRangesForRoom(roomId) {
  bookedRanges = [];
  const hint = document.getElementById("roomBlockedHint");
  if (hint) hint.textContent = "";
  if (!roomId) return;

  try {
    const res = await fetch(`${BASE}/api/reservations/by-room?roomId=${encodeURIComponent(roomId)}`, { credentials: "same-origin" });
    const txt = await res.text();
    let data = {};
    try { data = JSON.parse(txt); } catch (e) {}
    if (!res.ok) return;

    bookedRanges = data.bookings || [];
    if (bookedRanges.length && hint) {
      hint.textContent =
        `Blocked ranges: ` + bookedRanges.slice(0, 3).map(b => `${b.checkInDate} → ${b.checkOutDate}`).join(" , ")
        + (bookedRanges.length > 3 ? " ..." : "");
    }
  } catch (e) {
    bookedRanges = [];
  }
}

async function viewReservation(id) {
  try {
    const res = await fetch(`${BASE}/api/reservations/detail?id=${encodeURIComponent(id)}`, { credentials: "same-origin" });
    const txt = await res.text();
    let data = {};
    try { data = JSON.parse(txt); } catch (e) {}

    if (!res.ok) {
      showAlert("page", data.message || "Failed to load reservation detail.", "danger");
      return;
    }

    const r = data.reservation || data.data || data;
    if (!r || typeof r !== "object") {
      showAlert("page", "Invalid detail response from server.", "danger");
      return;
    }

    document.getElementById("vReservationNumber").textContent = r.reservationNumber || "-";
    setStatusBadge(document.getElementById("vStatus"), r.status);

    document.getElementById("vGuestName").textContent = r.guestName || ("Guest #" + (r.guestId ?? "-"));
    document.getElementById("vGuestEmail").textContent = r.guestEmail || "-";
    document.getElementById("vGuestPhone").textContent = r.guestContactNumber || "-";

    document.getElementById("vRoomNumber").textContent = r.roomNumber || ("Room #" + (r.roomId ?? "-"));
    document.getElementById("vDates").textContent = `${r.checkInDate || "-"} → ${r.checkOutDate || "-"}`;
    document.getElementById("vNights").textContent = `Nights: ${r.nights ?? "-"}`;

    document.getElementById("vRate").textContent = money(r.ratePerNight ?? 0);
    document.getElementById("vSubtotal").textContent = money(r.subtotal ?? 0);
    document.getElementById("vTax").textContent = money(r.tax ?? 0);
    document.getElementById("vTotal").textContent = money(r.totalAmount ?? 0);
    document.getElementById("vDiscount").textContent = `Discount: ${money(r.discount ?? 0)}`;

    document.getElementById("vPaid").textContent = money(r.amountPaid ?? 0);
    document.getElementById("vPayStatus").textContent = String(r.paymentStatus || "UNPAID");

    document.getElementById("vNotes").textContent = r.notes || "-";

    // payment button logic
    const paymentStatus = String(r.paymentStatus || "UNPAID").toUpperCase();
    const amountPaid = Number(r.amountPaid || 0);
    const totalAmount = Number(r.totalAmount || 0);
    const remaining = round2(totalAmount - amountPaid);

    const paymentBtn = document.getElementById("vPaymentBtn");
    if (paymentBtn) {
      if (paymentStatus !== "PAID" && remaining > 0) {
        paymentBtn.style.display = "inline-block";
        paymentBtn.onclick = () => {
          const vm = bootstrap.Modal.getInstance(document.getElementById("viewReservationModal"));
          if (vm) vm.hide();
          openPaymentModal(r.reservationId);
        };
      } else {
        paymentBtn.style.display = "none";
      }
    }

    document.getElementById("vEditBtn").onclick = () => {
      bootstrap.Modal.getOrCreateInstance(document.getElementById("viewReservationModal")).hide();
      openEditReservationModal(id);
    };

    document.getElementById("vDeleteBtn").onclick = async () => {
      bootstrap.Modal.getOrCreateInstance(document.getElementById("viewReservationModal")).hide();
      await deleteReservation(id);
    };

    bootstrap.Modal.getOrCreateInstance(document.getElementById("viewReservationModal")).show();

  } catch (e) {
    showAlert("page", "Network error: " + e.message, "danger");
  }
}

async function openEditReservationModal(id) {
  const r = reservationsCache.find(x => Number(x.reservationId) === Number(id));
  if (!r) { showAlert("page", "Reservation not found in table.", "warning"); return; }

  resetReservationForm();
  document.getElementById("reservationModalTitle").innerHTML =
    `<i class="fas fa-pen-to-square me-2 text-primary"></i>Edit Reservation`;
  document.getElementById("reservationId").value = r.reservationId;

  await loadRoomsDropdown();
  initGuestSearch();
  lockPastDates();
  bindPricingListeners();

  document.getElementById("guestId").value = r.guestId || "";
  document.getElementById("guestName").value = r.guestName || "";
  document.getElementById("guestEmail").value = r.guestEmail || "";
  document.getElementById("guestContactNumber").value = r.guestContactNumber || "";

  document.getElementById("roomId").value = r.roomId || "";
  const room = roomsById.get(String(r.roomId || ""));
  if (room) {
    const rate = getRoomRate(room);
    document.getElementById("roomMeta").textContent =
      `Max guests: ${room.maxGuests ?? 2} | Status: ${room.status} | Rate: ${money(rate)}`;
    await loadBookedRangesForRoom(room.roomId);
  }

  document.getElementById("checkIn").value = r.checkInDate || "";
  document.getElementById("checkOut").value = r.checkOutDate || "";
  document.getElementById("status").value = r.status || "PENDING";

  const savedSubtotal = Number(r.subtotal || 0);
  const savedTaxAmount = Number(r.tax || 0);
  const savedTaxRate = (r.taxRate != null)
    ? Number(r.taxRate)
    : (savedSubtotal > 0 ? round2((savedTaxAmount / savedSubtotal) * 100) : 0);

  document.getElementById("calcTax").value = String(Number.isFinite(savedTaxRate) ? savedTaxRate : 0);
  document.getElementById("calcDiscount").value = String(Number(r.discount || 0));

  const inD = document.getElementById("checkIn").value;
  if (inD) document.getElementById("checkOut").min = inD;

  recalcPricing();
  bootstrap.Modal.getOrCreateInstance(document.getElementById("reservationModal")).show();
}

async function deleteReservation(id) {
  if (!confirm("Delete this reservation?")) return;

  try {
    const res = await fetch(`${BASE}/api/reservations?id=${encodeURIComponent(id)}`, {
      method: "DELETE",
      credentials: "same-origin"
    });

    const txt = await res.text();
    let data = {};
    try { data = JSON.parse(txt); } catch (e) {}

    if (!res.ok) {
      showAlert("page", data.message || "Delete failed.", "danger");
      return;
    }

    showAlert("page", "Reservation deleted.", "success");
    loadReservations();
    if (calendar) calendar.refetchEvents();

  } catch (e) {
    showAlert("page", "Network error: " + e.message, "danger");
  }
}

async function saveReservation() {
  hideAlert("form");

  const reservationId = Number(document.getElementById("reservationId").value || 0);
  const roomId = Number(document.getElementById("roomId").value || 0);
  const room = roomsById.get(String(roomId));
  const rate = getRoomRate(room);

  const inD = document.getElementById("checkIn").value;
  const outD = document.getElementById("checkOut").value;

  const nights = calcNights(inD, outD);
  const subtotal = round2(nights * rate);

  const taxRate = getTaxRateInput();
  const discount = getDiscountInput();
  const tax = round2(subtotal * (taxRate / 100));
  let totalAmount = round2(subtotal + tax - discount);
  if (totalAmount < 0) totalAmount = 0;

  const payload = {
    reservationId,
    guestId: Number(document.getElementById("guestId").value || 0),
    guestName: document.getElementById("guestName").value.trim(),
    guestEmail: document.getElementById("guestEmail").value.trim(),
    guestContactNumber: document.getElementById("guestContactNumber").value.trim(),
    roomId,
    checkInDate: inD,
    checkOutDate: outD,
    guestCount: Number(document.getElementById("guestCount").value || 1),
    status: document.getElementById("status").value,
    notes: document.getElementById("notes").value.trim(),
    nights,
    ratePerNight: rate,
    subtotal,
    tax,
    taxRate,
    discount,
    totalAmount
  };

  if (!payload.guestName || !payload.guestEmail || !payload.guestContactNumber ||
    !payload.roomId || !payload.checkInDate || !payload.checkOutDate) {
    showAlert("form", "Please fill all required fields.", "warning");
    return;
  }

  const max = room?.maxGuests ?? 2;
  if (payload.guestCount > max) {
    showAlert("form", `Guest count cannot exceed room capacity (${max}).`, "warning");
    return;
  }

  if (!validateAgainstBooked()) return;

  try {
    setSaving(true);
    showAlert("form", reservationId ? "Updating reservation..." : "Saving reservation...", "info");

    const res = await fetch(`${BASE}/api/reservations`, {
      method: reservationId ? "PUT" : "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    const txt = await res.text();
    let data = {};
    try { data = JSON.parse(txt); } catch (e) {}

    if (!res.ok) {
      showAlert("form", data.message || "Failed to save reservation.", "danger");
      return;
    }

    showAlert("form", reservationId ? "Reservation updated successfully." : "Reservation saved successfully.", "success");

    setTimeout(() => {
      bootstrap.Modal.getOrCreateInstance(document.getElementById("reservationModal")).hide();
      loadReservations();
      if (calendar) calendar.refetchEvents();
    }, 400);

  } catch (err) {
    showAlert("form", "Network error: " + err.message, "danger");
  } finally {
    setSaving(false);
  }
}

function renderRecentCheckins() {
  const wrap = document.getElementById("recentCheckins");
  if (!wrap) return;

  const list = (reservationsCache || [])
    .filter(r => ["CHECKED_IN", "CONFIRMED"].includes(String(r.status || "").toUpperCase()))
    .sort((a, b) => new Date(b.checkInDate) - new Date(a.checkInDate))
    .slice(0, 6);

  const count = document.getElementById("checkInCount");
  if (count) count.textContent = String(list.length);

  if (!list.length) {
    wrap.innerHTML = `<div class="text-muted">No recent reservations.</div>`;
    return;
  }

  wrap.innerHTML = list.map(r => `
    <button class="btn btn-light border text-start ${r.status === 'CHECKED_IN' ? 'border-info' : 'border-success'}"
            type="button" onclick="viewReservation(${r.reservationId})">
      <div class="fw-semibold">${escapeHtml(r.guestName || ("Guest #" + (r.guestId || "")))}</div>
      <div class="small text-muted">
        Room ${escapeHtml(r.roomNumber || (r.roomId || ""))} •
        ${r.checkInDate || "-"} → ${r.checkOutDate || "-"}
      </div>
      <div class="small">
        <span class="badge ${
          r.status === 'CHECKED_IN' ? 'bg-info' :
          r.status === 'CONFIRMED' ? 'bg-success' : 'bg-secondary'
        }">${escapeHtml(r.status || "-")}</span>
      </div>
    </button>
  `).join("");
}

function tooltipText(ep, fallbackTitle, startStr, endStr) {
  return [
    `Reservation: ${ep.reservationNumber || fallbackTitle || "-"}`,
    `Guest: ${ep.guestName || "-"}`,
    `Phone: ${ep.guestContactNumber || "-"}`,
    `Room: ${ep.roomNumber || "-"}`,
    `Dates: ${(ep.checkInDate || startStr || "-")} → ${(ep.checkOutDate || endStr || "-")}`,
    `Status: ${ep.status || "-"}`
  ].join("\n");
}

function initCalendarIfNeeded() {
  if (calendar) return;

  const el = document.getElementById("bookingsCalendar");
  if (!el) return;

  calendar = new FullCalendar.Calendar(el, {
    initialView: "dayGridMonth",
    height: "auto",

    events: async (info, success, failure) => {
      try {
        const start = info.startStr.slice(0, 10);
        const end = info.endStr.slice(0, 10);

        const res = await fetch(`${BASE}/api/reservations/calendar?start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`, {
          credentials: "same-origin"
        });

        const txt = await res.text();
        let data = {};
        try { data = JSON.parse(txt); } catch (e) {}

        if (!res.ok) { failure(data); return; }

        const events = (data.events || []).map(e => ({
          id: e.id,
          title: e.title,
          start: e.start,
          end: e.end,
          extendedProps: e.extendedProps || {}
        }));

        success(events);
      } catch (e) {
        failure(e);
      }
    },

    eventMouseEnter: (info) => {
      const ep = info.event.extendedProps || {};
      info.el.setAttribute("title", tooltipText(ep, info.event.title, info.event.startStr, info.event.endStr));

      const t = bootstrap.Tooltip.getOrCreateInstance(info.el, {
        container: "body",
        trigger: "hover",
        placement: "top"
      });
      t.show();
    },

    eventMouseLeave: (info) => {
      const t = bootstrap.Tooltip.getInstance(info.el);
      if (t) t.dispose();
    },

    eventClick: (info) => {
      viewReservation(info.event.id);
    }
  });

  calendar.render();
}

async function loadReservations() {
  hideAlert("page");
  if (tbody) tbody.innerHTML = `<tr><td colspan="9" class="text-muted">Loading...</td></tr>`;

  try {
    const res = await fetch(`${BASE}/api/reservations`, { credentials: "same-origin" });
    const txt = await res.text();
    let data = {};
    try { data = JSON.parse(txt); } catch (e) {}

    if (!res.ok) {
      showAlert("page", data.message || "Failed to load reservations.", "danger");
      if (tbody) tbody.innerHTML = `<tr><td colspan="9" class="text-danger">Failed to load.</td></tr>`;
      return;
    }

    const list = data.reservations || [];
    reservationsCache = list;

    if (!list.length) {
      if (tbody) tbody.innerHTML = `<tr><td colspan="9" class="text-muted">No reservations found.</td></tr>`;
      renderRecentCheckins();
      return;
    }

    if (tbody) {
      tbody.innerHTML = list.map(r => `
        <tr>
          <td class="fw-semibold">${r.reservationId}</td>
          <td>${escapeHtml(r.guestName || String(r.guestId || ""))}</td>
          <td>${escapeHtml(r.guestEmail || "-")}</td>
          <td>${escapeHtml(r.roomNumber || String(r.roomId || ""))}</td>
          <td>${r.checkInDate}</td>
          <td>${r.checkOutDate}</td>
          <td>
            <span class="badge bg-${
              r.status === 'CONFIRMED' ? 'success' :
              r.status === 'PENDING' ? 'warning' :
              r.status === 'CHECKED_IN' ? 'info' : 'secondary'
            }">${escapeHtml(r.status || "-")}</span>
          </td>
          <td class="text-end">${(r.totalAmount != null) ? money(r.totalAmount) : "-"}</td>
          <td class="text-end text-nowrap">
            <button class="btn btn-outline-secondary btn-sm" onclick="viewReservation(${r.reservationId})">View</button>
            <button class="btn btn-outline-secondary btn-sm" onclick="openEditReservationModal(${r.reservationId})">Edit</button>
            <button class="btn btn-outline-primary btn-sm" onclick="openInvoice(${r.reservationId})">Invoice</button>
            <button class="btn btn-outline-danger btn-sm" onclick="deleteReservation(${r.reservationId})">Delete</button>
          </td>
        </tr>
      `).join("");
    }

    renderRecentCheckins();
    if (calendar) calendar.refetchEvents();

  } catch (err) {
    showAlert("page", "Network error: " + err.message, "danger");
    if (tbody) tbody.innerHTML = `<tr><td colspan="9" class="text-danger">Failed to load.</td></tr>`;
  }
}

function openInvoice(reservationId) {
  const url = BASE + "/api/invoice?reservationId=" + encodeURIComponent(reservationId);
  window.open(url, "_blank");
}

/* ------------------- PAYMENT (FIXED) ------------------- */

function showPaymentAlert(message, level = "danger") {
  showAlert("payment", message, level);
}

function openPaymentModal(reservationId) {
  hideAlert("payment");

  const modal = new bootstrap.Modal(document.getElementById("paymentModal"));
  document.getElementById("paymentReservationId").value = reservationId;
  document.getElementById("paymentForm").reset();

  fetch(`${BASE}/api/reservations/detail?id=${encodeURIComponent(reservationId)}`, { credentials: "same-origin" })
    .then(async r => {
      const txt = await r.text();
      let data = {};
      try { data = JSON.parse(txt); } catch (e) {}
      if (!r.ok) throw new Error(data.message || "Failed to load reservation details");
      return data;
    })
    .then(data => {
      const res = data.reservation;
      const remaining = round2(Number(res.totalAmount || 0) - Number(res.amountPaid || 0));

      document.getElementById("paymentRemaining").textContent = remaining.toFixed(2);

      const amt = document.getElementById("paymentAmount");
      amt.max = String(remaining);
      amt.value = remaining > 0 ? remaining.toFixed(2) : "0.00";
    })
    .catch(err => {
      showPaymentAlert(err.message, "danger");
    });

  loadPaymentHistory(reservationId);
  modal.show();
}

function loadPaymentHistory(reservationId) {
  const body = document.getElementById("paymentHistoryBody");
  if (body) body.innerHTML = `<tr><td colspan="4" class="text-muted">Loading...</td></tr>`;

  fetch(`${BASE}/api/payments/history?reservationId=${encodeURIComponent(reservationId)}`, { credentials: "same-origin" })
    .then(async r => {
      const txt = await r.text();
      let data = {};
      try { data = JSON.parse(txt); } catch (e) {}
      if (!r.ok) throw new Error(data.message || "Failed to load payment history");
      return data;
    })
    .then(data => {
      const tbodyEl = document.getElementById("paymentHistoryBody");
      if (!tbodyEl) return;

      tbodyEl.innerHTML = "";

      const payments = data.payments || [];
      if (!payments.length) {
        tbodyEl.innerHTML = `<tr><td colspan="4" class="text-muted">No payments yet</td></tr>`;
        return;
      }

      payments.forEach(p => {
        const paidDate = p.paidDate ? new Date(p.paidDate) : null;
        const dateText = paidDate && !isNaN(paidDate.getTime())
          ? paidDate.toLocaleString()
          : "-";

        const amt = Number(p.paidAmount || 0);

        const row = document.createElement("tr");
        row.innerHTML = `
          <td>${escapeHtml(dateText)}</td>
          <td>$${amt.toFixed(2)}</td>
          <td>${escapeHtml(p.method || "-")}</td>
          <td>${escapeHtml(p.note || "-")}</td>
        `;
        tbodyEl.appendChild(row);
      });
    })
    .catch(err => {
      const tbodyEl = document.getElementById("paymentHistoryBody");
      if (tbodyEl) tbodyEl.innerHTML = `<tr><td colspan="4" class="text-danger">${escapeHtml(err.message)}</td></tr>`;
    });
}

function savePayment() {
  hideAlert("payment");

  const reservationId = Number(document.getElementById("paymentReservationId").value || 0);
  const amount = parseFloat(document.getElementById("paymentAmount").value);
  const method = document.getElementById("paymentMethod").value;
  const note = document.getElementById("paymentNote").value;

  if (!reservationId) {
    showPaymentAlert("Reservation id missing.", "danger");
    return;
  }

  if (!Number.isFinite(amount) || amount <= 0) {
    showPaymentAlert("Please enter a valid amount.", "danger");
    return;
  }

  const remaining = parseFloat(document.getElementById("paymentRemaining").textContent || "0");
  if (Number.isFinite(remaining) && amount > remaining) {
    showPaymentAlert("Amount cannot be greater than remaining balance.", "warning");
    return;
  }

  const payload = {
    reservationId: reservationId,
    amount: amount,
    method: method,
    note: note
  };

  fetch(`${BASE}/api/payments`, {
    method: "POST",
    credentials: "same-origin",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  })
    .then(async r => {
      const txt = await r.text();
      let data = {};
      try { data = JSON.parse(txt); } catch (e) {}
      if (!r.ok) throw new Error(data.message || "Payment failed");
      return data;
    })
    .then(() => {
      showAlert("page", "Payment added successfully.", "success");
      document.getElementById("paymentForm").reset();

      loadPaymentHistory(reservationId);
      loadReservations();

      return fetch(`${BASE}/api/reservations/detail?id=${encodeURIComponent(reservationId)}`, { credentials: "same-origin" });
    })
    .then(async r => {
      const txt = await r.text();
      let data = {};
      try { data = JSON.parse(txt); } catch (e) {}
      if (!r.ok) throw new Error(data.message || "Failed to refresh remaining");

      const resv = data.reservation;
      const newRemaining = round2(Number(resv.totalAmount || 0) - Number(resv.amountPaid || 0));

      document.getElementById("paymentRemaining").textContent = newRemaining.toFixed(2);

      const amtEl = document.getElementById("paymentAmount");
      amtEl.max = String(newRemaining);
      amtEl.value = newRemaining > 0 ? newRemaining.toFixed(2) : "0.00";

      if (newRemaining <= 0) {
        const modal = bootstrap.Modal.getInstance(document.getElementById("paymentModal"));
        if (modal) modal.hide();
        showAlert("page", "Reservation fully paid.", "success");
      }
    })
    .catch(err => {
      showPaymentAlert(err.message, "danger");
    });
}
