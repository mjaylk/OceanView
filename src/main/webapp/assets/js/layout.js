// sidebar.js
document.addEventListener("DOMContentLoaded", async () => {
  const ctx = "/" + window.location.pathname.split("/")[1];

  const mount = document.getElementById("sidebarMount");
  if (mount) {
    const html = await fetch(ctx + "/partials/sidebar.html").then(r => r.text());
    mount.innerHTML = html;
  }

  const res = await fetch(ctx + "/api/me", { credentials: "include" });
  const data = await res.json();

  if (!data.success) {
    window.location.href = ctx + "/login.html";
    return;
  }

  const role = (data.role || "").toUpperCase();

  const subtitle = document.querySelector(".sidebar-subtitle");
  if (subtitle) subtitle.textContent = role === "STAFF" ? "Staff Panel" : "Admin Panel";

  document.querySelectorAll("[data-role]").forEach(el => {
    const allowed = (el.getAttribute("data-role") || "")
      .split(",")
      .map(s => s.trim().toUpperCase())
      .filter(Boolean);

    el.hidden = !allowed.includes(role);
  });

  const current = window.location.pathname.split("/").pop();
  document.querySelectorAll(".sidebar a.nav-link").forEach(a => {
    const href = a.getAttribute("href");
    if (href === current) a.classList.add("active");
  });

  loadSupportContactIntoSidebar(ctx);
});

function isMobile() {
  return window.matchMedia("(max-width: 992px)").matches;
}

function openSidebar() {
  const sidebar = document.getElementById("sidebar");
  const overlay = document.getElementById("sidebarOverlay");

  if (isMobile()) {
    sidebar && sidebar.classList.add("open");
    overlay && overlay.classList.add("show");
    document.body.classList.add("sidebar-open");
  } else {
    sidebar && sidebar.classList.remove("collapsed");
  }
}

function closeSidebar() {
  const sidebar = document.getElementById("sidebar");
  const overlay = document.getElementById("sidebarOverlay");

  if (isMobile()) {
    sidebar && sidebar.classList.remove("open");
    overlay && overlay.classList.remove("show");
    document.body.classList.remove("sidebar-open");
  } else {
    sidebar && sidebar.classList.add("collapsed");
  }
}

function toggleSidebar() {
  const sidebar = document.getElementById("sidebar");
  if (!sidebar) return;

  if (isMobile()) {
    sidebar.classList.contains("open") ? closeSidebar() : openSidebar();
  } else {
    sidebar.classList.contains("collapsed") ? openSidebar() : closeSidebar();
  }
}

function openHelpPanel() {
  const p = document.getElementById("helpPanel");
  if (p) p.classList.add("open");
}

function closeHelpPanel() {
  const p = document.getElementById("helpPanel");
  if (p) p.classList.remove("open");
}

async function loadSupportContactIntoSidebar(ctx) {
  const emailEl = document.getElementById("sbSupportEmail");
  const phoneEl = document.getElementById("sbSupportPhone");

  if (emailEl) emailEl.textContent = "-";
  if (phoneEl) phoneEl.textContent = "-";

  try {
    const res = await fetch(ctx + "/api/settings/by-category?category=CONTACT", { credentials: "include" });
    if (!res.ok) return;

    const data = await res.json();
    if (!data || !data.success || !Array.isArray(data.settings)) return;

    let email = "";
    let phone = "";

    for (const s of data.settings) {
      const k = (s.settingKey || "").toLowerCase();
      const v = (s.settingValue || "").trim();
      if (!v) continue;

      if (k === "contact_email" || k === "support_email" || k === "email") email = v;
      if (k === "contact_phone" || k === "support_phone" || k === "phone") phone = v;
    }

    if (emailEl) emailEl.textContent = email || "-";
    if (phoneEl) phoneEl.textContent = phone || "-";
  } catch (e) {
  }
}

document.addEventListener("click", (e) => {
  const panel = document.getElementById("helpPanel");
  const fab = document.querySelector(".help-fab");
  if (!panel || !fab) return;

  const clickedInside = panel.contains(e.target) || fab.contains(e.target);
  if (!clickedInside) panel.classList.remove("open");
});
