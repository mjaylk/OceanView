document.addEventListener("DOMContentLoaded", async () => {
  const ctx = "/" + window.location.pathname.split("/")[1];

  // Load sidebar HTML
  const mount = document.getElementById("sidebarMount");
  if (mount) {
    const html = await fetch(ctx + "/partials/sidebar.html").then(r => r.text());
    mount.innerHTML = html;
  }

  // Load role and apply permission rules
  const res = await fetch(ctx + "/api/me", { credentials: "include" });
  const data = await res.json();

  if (!data.success) {
    window.location.href = ctx + "/login.html";
    return;
  }

  const role = (data.role || "").toUpperCase();

  // Update label
  const subtitle = document.querySelector(".sidebar-subtitle");
  if (subtitle) subtitle.textContent = role === "STAFF" ? "Staff Panel" : "Admin Panel";

  // Apply role visibility
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
});
