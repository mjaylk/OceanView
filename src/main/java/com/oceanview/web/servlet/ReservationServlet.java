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

    // FullCalendar end is exclusive for all-day events -> add +1 day for rendering. [web:632][web:634]
    private String plusOneDay(Date d) {
        if (d == null) return "";
        return Date.valueOf(d.toLocalDate().plusDays(1)).toString();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!isStaffOrAdmin(req)) {
            sendJson(resp, 403, "{\"success\":false,\"message\":\"Staff/Admin access required\"}");
            return;
        }

        String path = req.getPathInfo();
        if (path == null) path = "";

        // ---------------------------
        // GET /api/reservations/by-room?roomId=1
        // ---------------------------
        if ("/by-room".equals(path)) {
            String roomIdStr = req.getParameter("roomId");
            if (roomIdStr == null || roomIdStr.trim().isEmpty()) {
                sendJson(resp, 400, "{\"success\":false,\"message\":\"roomId required\"}");
                return;
            }

            int roomId = Integer.parseInt(roomIdStr);
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

        // ---------------------------
        // GET /api/reservations/calendar?start=YYYY-MM-DD&end=YYYY-MM-DD&roomId=1(optional)
        // ---------------------------
        if ("/calendar".equals(path)) {
            String startStr = req.getParameter("start");
            String endStr = req.getParameter("end");

            if (startStr == null || endStr == null) {
                sendJson(resp, 400, "{\"success\":false,\"message\":\"start and end are required\"}");
                return;
            }

            Date start = Date.valueOf(startStr);
            Date end = Date.valueOf(endStr);

            String roomIdStr = req.getParameter("roomId");
            Integer roomId = (roomIdStr != null && !roomIdStr.trim().isEmpty()) ? Integer.valueOf(roomIdStr) : null;

            List<Reservation> list = service.getBetween(start, end);

            if (roomId != null) {
                list.removeIf(r -> r.getRoomId() != roomId);
            }

            StringBuilder sb = new StringBuilder("{\"success\":true,\"events\":[");
            for (int i = 0; i < list.size(); i++) {
                Reservation r = list.get(i);

                sb.append("{")
                  .append("\"id\":").append(r.getReservationId()).append(",")
                  .append("\"title\":\"").append(esc(r.getReservationNumber())).append("\",")

                  // FullCalendar end is exclusive -> checkout + 1 day for display. [web:632][web:634]
                  .append("\"start\":\"").append(r.getCheckInDate()).append("\",")
                  .append("\"end\":\"").append(plusOneDay(r.getCheckOutDate())).append("\",")
                  .append("\"allDay\":true,")

                  .append("\"extendedProps\":{")
                    .append("\"reservationNumber\":\"").append(esc(r.getReservationNumber())).append("\",")
                    .append("\"guestName\":\"").append(esc(r.getGuestName())).append("\",")
                    .append("\"guestContactNumber\":\"").append(esc(r.getGuestContactNumber())).append("\",")
                    .append("\"roomNumber\":\"").append(esc(r.getRoomNumber())).append("\",")
                    .append("\"checkInDate\":\"").append(r.getCheckInDate()).append("\",")

                    // Keep real checkout date here (not +1)
                    .append("\"checkOutDate\":\"").append(r.getCheckOutDate()).append("\",")
                    .append("\"status\":\"").append(esc(r.getStatus())).append("\"")
                  .append("}")
                  .append("}");

                if (i < list.size() - 1) sb.append(",");
            }
            sb.append("]}");
            sendJson(resp, 200, sb.toString());
            return;
        }

        // ---------------------------
        // GET /api/reservations/detail?id=123
        // ---------------------------
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
                    "\"guestName\":\"" + esc(r.getGuestName()) + "\"," +
                    "\"guestEmail\":\"" + esc(r.getGuestEmail()) + "\"," +
                    "\"guestContactNumber\":\"" + esc(r.getGuestContactNumber()) + "\"," +
                    "\"roomNumber\":\"" + esc(r.getRoomNumber()) + "\"," +
                    "\"roomType\":\"" + esc(r.getRoomType()) + "\"," +
                    "\"checkInDate\":\"" + r.getCheckInDate() + "\"," +
                    "\"checkOutDate\":\"" + r.getCheckOutDate() + "\"," +
                    "\"status\":\"" + esc(r.getStatus()) + "\"," +
                    "\"nights\":" + r.getNights() + "," +
                    "\"ratePerNight\":" + r.getRatePerNight() + "," +
                    "\"subtotal\":" + r.getSubtotal() + "," +
                    "\"tax\":" + r.getTax() + "," +
                    "\"discount\":" + r.getDiscount() + "," +
                    "\"totalAmount\":" + r.getTotalAmount() +
                "}}"
            );
            return;
        }

        // ---------------------------
        // GET /api/reservations  (list)
        // ---------------------------
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
              .append("\"nights\":").append(r.getNights()).append(",")
              .append("\"totalAmount\":").append(r.getTotalAmount()).append(",")
              .append("\"guestName\":\"").append(esc(r.getGuestName())).append("\",")
              .append("\"guestEmail\":\"").append(esc(r.getGuestEmail())).append("\",")
              .append("\"guestContactNumber\":\"").append(esc(r.getGuestContactNumber())).append("\",")
              .append("\"roomNumber\":\"").append(esc(r.getRoomNumber())).append("\"")
              .append("}");
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]}");
        sendJson(resp, 200, sb.toString());
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!isStaffOrAdmin(req)) { sendJson(resp, 403, "{\"success\":false,\"message\":\"Staff/Admin access required\"}"); return; }
        User user = sessionUser(req);
        if (user == null) { sendJson(resp, 401, "{\"success\":false,\"message\":\"Session expired. Please login again.\"}"); return; }

        String body = getBody(req);

        try {
            int reservationId = Integer.parseInt(extract(body, "reservationId", "0"));
            int guestId = Integer.parseInt(extract(body, "guestId", "0"));
            String guestName = extract(body, "guestName", "");
            String guestEmail = extract(body, "guestEmail", "");
            String guestPhone = extract(body, "guestContactNumber", "");
            int roomId = Integer.parseInt(extract(body, "roomId", "0"));
            Date checkIn = Date.valueOf(extract(body, "checkInDate", ""));
            Date checkOut = Date.valueOf(extract(body, "checkOutDate", ""));
            String status = extract(body, "status", "PENDING");

            boolean ok = service.updateReservationSmart(
                reservationId, guestId, guestName, guestEmail, guestPhone,
                roomId, checkIn, checkOut, status, user.getUserId()
            );

            if (!ok) { sendJson(resp, 400, "{\"success\":false,\"message\":\"Update failed\"}"); return; }
            sendJson(resp, 200, "{\"success\":true}");

        } catch (Exception e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
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
                guestId, guestName, guestEmail, guestPhone,
                roomId, checkIn, checkOut, status, user.getUserId()
            );

            sendJson(resp, 201, "{\"success\":true,\"reservationId\":" + id + "}");
        } catch (Exception e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!isStaffOrAdmin(req)) { sendJson(resp, 403, "{\"success\":false,\"message\":\"Staff/Admin access required\"}"); return; }
        User user = sessionUser(req);
        if (user == null) { sendJson(resp, 401, "{\"success\":false,\"message\":\"Session expired. Please login again.\"}"); return; }

        try {
            int id = Integer.parseInt(req.getParameter("id"));
            boolean ok = service.deleteReservation(id);
            if (!ok) { sendJson(resp, 404, "{\"success\":false,\"message\":\"Reservation not found\"}"); return; }
            sendJson(resp, 200, "{\"success\":true}");
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
