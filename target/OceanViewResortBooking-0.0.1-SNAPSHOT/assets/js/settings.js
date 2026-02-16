const BASE = "/OceanViewResortBooking";

document.addEventListener("DOMContentLoaded", () => {
  document.getElementById("todayLabel").textContent = new Date().toLocaleDateString();
  loadAllSettings();
});

function showAlert(message, level = "info") {
  const wrap = document.getElementById("pageAlertWrap");
  const alert = document.getElementById("pageAlert");
  const msg = document.getElementById("pageAlertMsg");
  
  wrap.style.display = "block";
  alert.className = `alert alert-${level} alert-dismissible fade show`;
  msg.textContent = message;
  
  setTimeout(() => {
    wrap.style.display = "none";
  }, 5000);
}

function hideAlert() {
  document.getElementById("pageAlertWrap").style.display = "none";
}

async function loadAllSettings() {
  try {
    const res = await fetch(`${BASE}/api/settings`, { credentials: "same-origin" });
    const txt = await res.text();
    let data = {};
    try { data = JSON.parse(txt); } catch (e) {}
    
    if (!res.ok) {
      showAlert(data.message || "Failed to load settings", "danger");
      return;
    }
    
    const settings = data.settings || [];
    
    settings.forEach(setting => {
      const input = document.getElementById(setting.settingKey);
      if (input) {
        input.value = setting.settingValue || "";
      }
    });
    
  } catch (err) {
    showAlert("Network error: " + err.message, "danger");
  }
}

async function saveSettings(category) {
  hideAlert();
  
  const settingsMap = {};
  let formId = "";
  
  switch (category) {
    case "EMAIL":
      formId = "emailForm";
      settingsMap.smtp_host = document.getElementById("smtp_host").value;
      settingsMap.smtp_port = document.getElementById("smtp_port").value;
      settingsMap.smtp_username = document.getElementById("smtp_username").value;
      settingsMap.smtp_password = document.getElementById("smtp_password").value;
      settingsMap.smtp_from_email = document.getElementById("smtp_from_email").value;
      settingsMap.smtp_from_name = document.getElementById("smtp_from_name").value;
      settingsMap.smtp_use_tls = document.getElementById("smtp_use_tls").value;
      settingsMap.smtp_use_auth = document.getElementById("smtp_use_auth").value;
      break;
      
    case "GENERAL":
      formId = "generalForm";
      settingsMap.app_name = document.getElementById("app_name").value;
      settingsMap.app_timezone = document.getElementById("app_timezone").value;
      settingsMap.currency_code = document.getElementById("currency_code").value;
      settingsMap.currency_symbol = document.getElementById("currency_symbol").value;
      settingsMap.tax_rate = document.getElementById("tax_rate").value;
      break;
      
    case "BOOKING":
      formId = "bookingForm";
      settingsMap.booking_advance_days = document.getElementById("booking_advance_days").value;
      settingsMap.cancellation_hours = document.getElementById("cancellation_hours").value;
      break;
      
    case "CONTACT":
      formId = "contactForm";
      settingsMap.contact_phone = document.getElementById("contact_phone").value;
      settingsMap.contact_email = document.getElementById("contact_email").value;
      settingsMap.contact_address = document.getElementById("contact_address").value;
      break;
  }
  
  if (!validateSettings(settingsMap, category)) {
    return;
  }
  
  try {
    const res = await fetch(`${BASE}/api/settings`, {
      method: "PUT",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(settingsMap)
    });
    
    const txt = await res.text();
    let data = {};
    try { data = JSON.parse(txt); } catch (e) {}
    
    if (!res.ok) {
      showAlert(data.message || "Failed to save settings", "danger");
      return;
    }
    
    showAlert("Settings saved successfully!", "success");
    
  } catch (err) {
    showAlert("Network error: " + err.message, "danger");
  }
}

function validateSettings(settings, category) {
  if (category === "EMAIL") {
    if (!settings.smtp_host || !settings.smtp_host.trim()) {
      showAlert("SMTP host is required", "warning");
      return false;
    }
    if (!settings.smtp_port || !settings.smtp_port.trim()) {
      showAlert("SMTP port is required", "warning");
      return false;
    }
    const port = parseInt(settings.smtp_port);
    if (isNaN(port) || port < 1 || port > 65535) {
      showAlert("SMTP port must be between 1 and 65535", "warning");
      return false;
    }
    if (!settings.smtp_from_email || !settings.smtp_from_email.trim()) {
      showAlert("From email is required", "warning");
      return false;
    }
    if (!isValidEmail(settings.smtp_from_email)) {
      showAlert("From email is not valid", "warning");
      return false;
    }
  }
  
  if (category === "GENERAL") {
    if (!settings.app_name || !settings.app_name.trim()) {
      showAlert("Application name is required", "warning");
      return false;
    }
    if (settings.currency_code && settings.currency_code.length !== 3) {
      showAlert("Currency code must be 3 characters", "warning");
      return false;
    }
    const taxRate = parseFloat(settings.tax_rate);
    if (isNaN(taxRate) || taxRate < 0 || taxRate > 100) {
      showAlert("Tax rate must be between 0 and 100", "warning");
      return false;
    }
  }
  
  if (category === "BOOKING") {
    const advanceDays = parseInt(settings.booking_advance_days);
    if (isNaN(advanceDays) || advanceDays < 1) {
      showAlert("Booking advance days must be at least 1", "warning");
      return false;
    }
    const cancelHours = parseInt(settings.cancellation_hours);
    if (isNaN(cancelHours) || cancelHours < 1) {
      showAlert("Cancellation hours must be at least 1", "warning");
      return false;
    }
  }
  
  if (category === "CONTACT") {
    if (!settings.contact_email || !settings.contact_email.trim()) {
      showAlert("Contact email is required", "warning");
      return false;
    }
    if (!isValidEmail(settings.contact_email)) {
      showAlert("Contact email is not valid", "warning");
      return false;
    }
  }
  
  return true;
}

function isValidEmail(email) {
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return re.test(email);
}

async function testSmtpConnection() {
  hideAlert();
  
  const host = document.getElementById("smtp_host").value;
  const port = document.getElementById("smtp_port").value;
  
  if (!host || !host.trim()) {
    showAlert("Please enter SMTP host before testing", "warning");
    return;
  }
  
  if (!port || !port.trim()) {
    showAlert("Please enter SMTP port before testing", "warning");
    return;
  }
  
  showAlert("Testing SMTP connection... (This feature requires backend implementation)", "info");
  
  setTimeout(() => {
    showAlert("SMTP test: Connection validation passed (simulation)", "success");
  }, 1500);
}

function togglePasswordVisibility(fieldId) {
  const field = document.getElementById(fieldId);
  const icon = event.target;
  
  if (field.type === "password") {
    field.type = "text";
    icon.classList.remove("fa-eye");
    icon.classList.add("fa-eye-slash");
  } else {
    field.type = "password";
    icon.classList.remove("fa-eye-slash");
    icon.classList.add("fa-eye");
  }
}