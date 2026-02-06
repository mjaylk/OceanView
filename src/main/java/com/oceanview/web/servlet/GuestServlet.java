package com.oceanview.web.servlet;

import com.oceanview.model.Guest;
import com.oceanview.model.User;
import com.oceanview.service.GuestService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/api/guests/*")
public class GuestServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private GuestService service;

    @Override
    public void init() {
        service = new GuestService();
    }

    private boolean isStaffOrAdmin(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return false;
        User user = (User) session.getAttribute("user");
        if (user == null) return false;
        String role = user.getRole();
        return "ADMIN".equalsIgnoreCase(role) || "STAFF".equalsIgnoreCase(role);
    }

    private void sendJson(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(json);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!isStaffOrAdmin(req)) {
            sendJson(resp, 403, "{\"success\":false,\"message\":\"Staff/Admin access required\"}");
            return;
        }

        String path = req.getPathInfo();

        // DETAIL: /api/guests/detail?id=1
        if ("/detail".equals(path)) {
            String idStr = req.getParameter("id");

            if (idStr == null || idStr.trim().isEmpty()) {
                sendJson(resp, 400, "{\"success\":false,\"message\":\"id required\"}");
                return;
            }

            try {
                int id = Integer.parseInt(idStr.trim());
                Guest g = service.getGuestById(id);
                if (g == null) {
                    sendJson(resp, 404, "{\"success\":false,\"message\":\"Guest not found\"}");
                    return;
                }
                sendGuestObject(resp, g);
                return;
            } catch (NumberFormatException e) {
                sendJson(resp, 400, "{\"success\":false,\"message\":\"Invalid id\"}");
                return;
            }
        }

        // SEARCH: /api/guests/search?q=xx
        if ("/search".equals(path)) {
            String q = req.getParameter("q");
            List<Guest> guests = service.searchGuests(q);

            StringBuilder sb = new StringBuilder("{\"success\":true,\"guests\":[");
            for (int i = 0; i < guests.size(); i++) {
                Guest g = guests.get(i);
                sb.append("{")
                        .append("\"guestId\":").append(g.getGuestId()).append(",")
                        .append("\"fullName\":\"").append(esc(g.getFullName())).append("\",")
                        .append("\"email\":\"").append(esc(g.getEmail())).append("\",")
                        .append("\"address\":\"").append(esc(g.getAddress())).append("\",")
                        .append("\"contactNumber\":\"").append(esc(g.getContactNumber())).append("\"")
                        .append("}");
                if (i < guests.size() - 1) sb.append(",");
            }
            sb.append("]}");

            sendJson(resp, 200, sb.toString());
            return;
        }

       
        String email = req.getParameter("email");
        String phone = req.getParameter("phone");

        if (email != null && !email.trim().isEmpty()) {
            Guest g = service.getGuestByEmail(email.trim());
            if (g == null) {
                sendJson(resp, 404, "{\"success\":false,\"message\":\"Guest not found\"}");
                return;
            }
            sendGuestObject(resp, g);
            return;
        }

        if (phone != null && !phone.trim().isEmpty()) {
            Guest g = service.getGuestByContactNumber(phone.trim());
            if (g == null) {
                sendJson(resp, 404, "{\"success\":false,\"message\":\"Guest not found\"}");
                return;
            }
            sendGuestObject(resp, g);
            return;
        }

        // LIST: /api/guests  (THIS PART WAS MISSING "address")
        List<Guest> list = service.listGuests();
        StringBuilder sb = new StringBuilder("{\"success\":true,\"guests\":[");
        for (int i = 0; i < list.size(); i++) {
            Guest g = list.get(i);
            sb.append("{")
                    .append("\"guestId\":").append(g.getGuestId()).append(",")
                    .append("\"fullName\":\"").append(esc(g.getFullName())).append("\",")
                    .append("\"email\":\"").append(esc(g.getEmail())).append("\",")
                    .append("\"address\":\"").append(esc(g.getAddress())).append("\",")
                    .append("\"contactNumber\":\"").append(esc(g.getContactNumber())).append("\"")
                    .append("}");
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]}");
        sendJson(resp, 200, sb.toString());
    }

    private void sendGuestObject(HttpServletResponse resp, Guest g) throws IOException {
        sendJson(resp, 200,
                "{\"success\":true,\"guest\":{" +
                        "\"guestId\":" + g.getGuestId() + "," +
                        "\"fullName\":\"" + esc(g.getFullName()) + "\"," +
                        "\"address\":\"" + esc(g.getAddress()) + "\"," +
                        "\"contactNumber\":\"" + esc(g.getContactNumber()) + "\"," +
                        "\"email\":\"" + esc(g.getEmail()) + "\"" +
                        "}}"
        );
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!isStaffOrAdmin(req)) {
            sendJson(resp, 403, "{\"success\":false,\"message\":\"Staff/Admin access required\"}");
            return;
        }

        String body = getBody(req);

        try {
            Integer userId = null;
            String fullName = extract(body, "fullName", "").trim();
            String address = extract(body, "address", "").trim();
            String contactNumber = extract(body, "contactNumber", "").trim();
            String email = extract(body, "email", "").trim();

            if (fullName.isEmpty()) throw new IllegalArgumentException("Full name required");
            if (contactNumber.isEmpty()) throw new IllegalArgumentException("Contact number required");

            int id = service.createGuest(userId, fullName, address, contactNumber, email, email);
            sendJson(resp, 201, "{\"success\":true,\"guestId\":" + id + "}");
        } catch (Exception e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!isStaffOrAdmin(req)) {
            sendJson(resp, 403, "{\"success\":false,\"message\":\"Staff/Admin access required\"}");
            return;
        }

        String body = getBody(req);

        try {
            String guestIdStr = extract(body, "guestId", "").trim();
            if (guestIdStr.isEmpty()) throw new IllegalArgumentException("guestId required");

            int guestId = Integer.parseInt(guestIdStr);

            String fullName = extract(body, "fullName", "").trim();
            String address = extract(body, "address", "").trim();
            String contactNumber = extract(body, "contactNumber", "").trim();
            String email = extract(body, "email", "").trim();

            if (fullName.isEmpty()) throw new IllegalArgumentException("Full name required");
            if (contactNumber.isEmpty()) throw new IllegalArgumentException("Contact number required");

            boolean ok = service.updateGuest(guestId, fullName, address, contactNumber, email);
            if (!ok) {
                sendJson(resp, 404, "{\"success\":false,\"message\":\"Guest not found\"}");
                return;
            }

            sendJson(resp, 200, "{\"success\":true}");
        } catch (NumberFormatException e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"Invalid guestId\"}");
        } catch (Exception e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!isStaffOrAdmin(req)) {
            sendJson(resp, 403, "{\"success\":false,\"message\":\"Staff/Admin access required\"}");
            return;
        }

        try {
            String idStr = req.getParameter("id");

            if (idStr == null || idStr.trim().isEmpty()) {
                sendJson(resp, 400, "{\"success\":false,\"message\":\"id required\"}");
                return;
            }

            int id = Integer.parseInt(idStr.trim());

            boolean ok = service.deleteGuest(id);
            if (!ok) {
                sendJson(resp, 404, "{\"success\":false,\"message\":\"Guest not found\"}");
                return;
            }

            sendJson(resp, 200, "{\"success\":true}");
        } catch (NumberFormatException e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"Invalid id\"}");
        } catch (Exception e) {
            sendJson(resp, 500, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private String getBody(HttpServletRequest req) throws IOException {
        java.util.Scanner s = new java.util.Scanner(req.getInputStream()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private String extract(String json, String key, String defaultVal) {
        if (json == null) return defaultVal;

        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return defaultVal;

        start += search.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length()) return defaultVal;

        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf('"', start);
            return end != -1 ? json.substring(start, end) : defaultVal;
        }

        int end = start;
        while (end < json.length() && ",}\n\r\t ".indexOf(json.charAt(end)) == -1) end++;

        String val = json.substring(start, end).trim();
        if (val.isEmpty() || "null".equalsIgnoreCase(val)) return defaultVal;
        return val;
    }
}
