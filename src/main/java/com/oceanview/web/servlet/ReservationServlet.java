package com.oceanview.web.servlet;

import com.oceanview.model.Reservation;
import com.oceanview.model.User;
import com.oceanview.service.ReservationService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Date;
import java.util.List;

@WebServlet("/api/reservations/*")
public class ReservationServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private ReservationService service;

    @Override
    public void init() {
        service = new ReservationService();
    }

    private boolean isStaffOrAdmin(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return false;
        User user = (User) session.getAttribute("user");
        if (user == null) return false;
        String role = user.getRole();
        return "ADMIN".equalsIgnoreCase(role) || "STAFF".equalsIgnoreCase(role);
    }

    private User sessionUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object u = session.getAttribute("user");
        return (u instanceof User) ? (User) u : null;
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

        String path = req.getPathInfo(); // null, "/by-room", "/calendar", "/detail"

        // 1) /by-room?roomId=1  -> for blocked ranges
        if ("/by-room".equals(path)) {
            int roomId = Integer.parseInt(req.getParameter("roomId"));
            List<Reservation> list = service.getBookedByRoom(roomId);

            StringBuilder sb = new StringBuilder("{\"success\":true,\"bookings\":[");
            for (int i = 0; i < list.size(); i++) {
                Reservation r = list.get(i);
                sb.append("{")
                  .append("\"reservationId\":").append(r.getReservationId()).append(",")
                  .append("\"checkInDate\":\"").append(r.getCheckInDate()).append("\",")
                  .append("\"checkOutDate\":\"").append(r.getCheckOutDate()).append("\"")
                  .append("}");
                if (i < list.size() - 1) sb.append(",");
            }
            sb.append("]}");
            sendJson(resp, 200, sb.toString());
            return;
        }

        // 2) /calendar?start=YYYY-MM-DD&end=YYYY-MM-DD  -> for calendar events
        if ("/calendar".equals(path)) {
            Date start = Date.valueOf(req.getParameter("start"));
            Date end = Date.valueOf(req.getParameter("end"));
            List<Reservation> list = service.getBetween(start, end);

            // simple events (you can format later for FullCalendar)
            StringBuilder sb = new StringBuilder("{\"success\":true,\"events\":[");
            for (int i = 0; i < list.size(); i++) {
                Reservation r = list.get(i);
                sb.append("{")
                  .append("\"id\":").append(r.getReservationId()).append(",")
                  .append("\"title\":\"").append(esc(r.getReservationNumber())).append("\",")
                  .append("\"start\":\"").append(r.getCheckInDate()).append("\",")
                  .append("\"end\":\"").append(r.getCheckOutDate()).append("\"")
                  .append("}");
                if (i < list.size() - 1) sb.append(",");
            }
            sb.append("]}");
            sendJson(resp, 200, sb.toString());
            return;
        }

        // 3) /detail?id=123 -> popup detail
        if ("/detail".equals(path)) {
            int id = Integer.parseInt(req.getParameter("id"));
            Reservation r = service.getById(id);
            if (r == null) {
                sendJson(resp, 404, "{\"success\":false,\"message\":\"Reservation not found\"}");
                return;
            }

            sendJson(resp, 200,
                "{\"success\":true,\"reservation\":{" +
                    "\"reservationId\":" + r.getReservationId() + "," +
                    "\"reservationNumber\":\"" + esc(r.getReservationNumber()) + "\"," +
                    "\"guestId\":" + r.getGuestId() + "," +
                    "\"roomId\":" + r.getRoomId() + "," +
                    "\"checkInDate\":\"" + r.getCheckInDate() + "\"," +
                    "\"checkOutDate\":\"" + r.getCheckOutDate() + "\"," +
                    "\"status\":\"" + esc(r.getStatus()) + "\"" +
                "}}"
            );
            return;
        }

        // default: list all
        List<Reservation> list = service.listReservations();
        StringBuilder sb = new StringBuilder("{\"success\":true,\"reservations\":[");
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            sb.append("{")
              .append("\"reservationId\":").append(r.getReservationId()).append(",")
              .append("\"reservationNumber\":\"").append(esc(r.getReservationNumber())).append("\",")
              .append("\"guestId\":").append(r.getGuestId()).append(",")
              .append("\"roomId\":").append(r.getRoomId()).append(",")
              .append("\"checkInDate\":\"").append(r.getCheckInDate()).append("\",")
              .append("\"checkOutDate\":\"").append(r.getCheckOutDate()).append("\",")
              .append("\"status\":\"").append(esc(r.getStatus())).append("\",")

              // optional display fields if JOIN worked
              .append("\"guestName\":\"").append(esc(r.getGuestName())).append("\",")
              .append("\"guestEmail\":\"").append(esc(r.getGuestEmail())).append("\",")
              .append("\"roomNumber\":\"").append(esc(r.getRoomNumber())).append("\"")

              .append("}");
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]}");
        sendJson(resp, 200, sb.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!isStaffOrAdmin(req)) {
            sendJson(resp, 403, "{\"success\":false,\"message\":\"Staff/Admin access required\"}");
            return;
        }

        User user = sessionUser(req);
        if (user == null) {
            sendJson(resp, 401, "{\"success\":false,\"message\":\"Session expired. Please login again.\"}");
            return;
        }

        String body = getBody(req);

        try {
            int guestId = Integer.parseInt(extract(body, "guestId", "0"));

            String guestName = extract(body, "guestName", "");
            String guestEmail = extract(body, "guestEmail", "");
            String guestPhone = extract(body, "guestContactNumber", "");

            int roomId = Integer.parseInt(extract(body, "roomId", "0"));
            Date checkIn = Date.valueOf(extract(body, "checkInDate", ""));
            Date checkOut = Date.valueOf(extract(body, "checkOutDate", ""));
            String status = extract(body, "status", "PENDING");

            int id = service.createReservationSmart(
                    guestId,
                    guestName,
                    guestEmail,
                    guestPhone,
                    roomId,
                    checkIn,
                    checkOut,
                    status,
                    user.getUserId()
            );

            sendJson(resp, 201, "{\"success\":true,\"reservationId\":" + id + "}");
        } catch (Exception e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
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
