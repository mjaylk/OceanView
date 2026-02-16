document.addEventListener("DOMContentLoaded", async () => {
  try {
 
    const ctx = "/" + window.location.pathname.split("/")[1];

    console.log("ROLE_UI: ctx =", ctx);

    const res = await fetch(ctx + "/api/me", { credentials: "include" });
    console.log("ROLE_UI: /api/me status =", res.status);

    const data = await res.json();
    console.log("ROLE_UI: /api/me data =", data);

    if (!data.success) {
      console.log("ROLE_UI: not logged in, redirect to login");
      window.location.href = ctx + "/login.html";
      return;
    }

    const role = (data.role || "").toUpperCase();
    console.log("ROLE_UI: role =", role);

    // Change subtitle label
    const subtitle = document.querySelector(".sidebar-subtitle");
    if (subtitle) subtitle.textContent = role === "STAFF" ? "Staff Panel" : "Admin Panel";


    const nodes = document.querySelectorAll("[data-role]");
    console.log("ROLE_UI: nodes found =", nodes.length);

    nodes.forEach((el) => {
      const allowed = (el.getAttribute("data-role") || "")
        .split(",")
        .map(s => s.trim().toUpperCase())
        .filter(Boolean);

      const ok = allowed.includes(role);

  
      el.hidden = !ok;
      el.style.display = ok ? "" : "none";
    });

    console.log("ROLE_UI: done");

  } catch (e) {
    console.error("ROLE_UI ERROR:", e);
  }
});
