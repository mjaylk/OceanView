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

// Flash message system - GLOBAL FUNCTIONS
function createFlashBox(type, msg) {
  var bg = type === "success" ? "#d4edda" : "#f8d7da";
  var bd = type === "success" ? "#c3e6cb" : "#f5c6cb";
  var tx = type === "success" ? "#155724" : "#721c24";
  var icon = type === "success" ? "fas fa-check-circle" : "fas fa-exclamation-circle";

  return (
    '<div class="flash-message flash-' + type + '" style="' +
    'padding: 15px 20px;' +
    'border: 1px solid ' + bd + ';' +
    'background: ' + bg + ';' +
    'color: ' + tx + ';' +
    'border-radius: 8px;' +
    'display: flex;' +
    'align-items: center;' +
    'gap: 12px;' +
    'box-shadow: 0 4px 12px rgba(0,0,0,0.15);' +
    'font-size: 14px;' +
    'font-weight: 500;' +
    'animation: slideInRight 0.4s ease-out;' +
    'margin-bottom: 10px;' +
    '">' +
    '<i class="' + icon + '" style="font-size: 18px;"></i>' +
    '<span style="flex: 1;">' + msg + '</span>' +
    '<button onclick="this.parentElement.remove()" style="' +
    'background: transparent;' +
    'border: none;' +
    'color: ' + tx + ';' +
    'font-size: 20px;' +
    'cursor: pointer;' +
    'padding: 0;' +
    'width: 24px;' +
    'height: 24px;' +
    'opacity: 0.6;' +
    '" onmouseover="this.style.opacity=\'1\'" onmouseout="this.style.opacity=\'0.6\'">' +
    '&times;' +
    '</button>' +
    '</div>'
  );
}

// EXPOSE THIS GLOBALLY SO OTHER PAGES CAN CALL IT
window.loadFlashMessages = function() {
  fetch("api/flash", { 
    cache: "no-store",
    headers: {
      'Cache-Control': 'no-cache'
    }
  })
  .then(function (r) { 
    if (!r.ok) throw new Error('Network response was not ok');
    return r.json(); 
  })
  .then(function (d) {
    console.log("Flash response:", d);
    
    var area = document.getElementById("flashArea");
    if (!area) {
      console.warn("flashArea element not found");
      return;
    }

    var html = "";
    
    // IMPORTANT: Only show ONE message - prioritize error over success
    // This prevents showing both old error + new success
    if (d.error && typeof d.error === 'string' && d.error.length > 0) {
      html += createFlashBox("error", d.error);
    } else if (d.success && typeof d.success === 'string' && d.success.length > 0) {
      html += createFlashBox("success", d.success);
    }

    if (html) {
      console.log("Displaying flash message");
      area.innerHTML = html;
      
      setTimeout(function() {
        var messages = area.querySelectorAll('.flash-message');
        messages.forEach(function(msg) {
          msg.classList.add('fade-out');
          setTimeout(function() {
            msg.remove();
          }, 500);
        });
      }, 5000);
    } else {
      console.log("No flash messages to display");
    }
  })
  .catch(function (err) {
    console.error("Flash fetch error:", err);
  });
};

// Auto-load flash on page load
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', window.loadFlashMessages);
} else {
  window.loadFlashMessages();
}

// EXPOSE THIS GLOBALLY SO OTHER PAGES CAN CALL IT
window.loadFlashMessages = function() {
  fetch("api/flash", { 
    cache: "no-store",
    headers: {
      'Cache-Control': 'no-cache'
    }
  })
  .then(function (r) { 
    if (!r.ok) throw new Error('Network response was not ok');
    return r.json(); 
  })
  .then(function (d) {
    console.log("Flash response:", d);
    
    var area = document.getElementById("flashArea");
    if (!area) {
      console.warn("flashArea element not found");
      return;
    }

    var html = "";
    
    if (d.success && typeof d.success === 'string' && d.success.length > 0) {
      html += createFlashBox("success", d.success);
    }
    
    if (d.error && typeof d.error === 'string' && d.error.length > 0) {
      html += createFlashBox("error", d.error);
    }

    if (html) {
      console.log("Displaying flash message");
      area.innerHTML = html;
      
      setTimeout(function() {
        var messages = area.querySelectorAll('.flash-message');
        messages.forEach(function(msg) {
          msg.classList.add('fade-out');
          setTimeout(function() {
            msg.remove();
          }, 500);
        });
      }, 5000);
    } else {
      console.log("No flash messages to display");
    }
  })
  .catch(function (err) {
    console.error("Flash fetch error:", err);
  });
};

// Auto-load flash on page load
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', window.loadFlashMessages);
} else {
  window.loadFlashMessages();
}