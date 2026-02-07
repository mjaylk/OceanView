package com.oceanview.web.servlet;

import com.oceanview.model.Room;
import com.oceanview.model.User;
import com.oceanview.service.ReservationService;
import com.oceanview.service.RoomService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Date;
import java.util.List;

@WebServlet("/api/rooms/*")
public class RoomServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // service objects
    private RoomService roomService;
    private ReservationService reservationService;

    @Override
    public void init() {
        roomService = new RoomService();
        reservationService = new ReservationService();
    }

    // get user from session
    private User sessionUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object user = session.getAttribute("user");
        return (user instanceof User) ? (User) user : null;
    }

    // admin check
    private boolean isAdmin(HttpServletRequest req) {
        User u = sessionUser(req);
        if (u == null) return false;
        return "ADMIN".equalsIgnoreCase(u.getRole());
    }

    // staff or admin check
    private boolean isStaffOrAdmin(HttpServletRequest req) {
        User u = sessionUser(req);
        if (u == null) return false;
        String role = u.getRole();
        return "ADMIN".equalsIgnoreCase(role) || "STAFF".equalsIgnoreCase(role);
    }

    // send json response
    private void sendJson(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(json);
    }

    // deny access
    private void denyAccess(HttpServletResponse resp, String message) throws IOException {
        sendJson(resp, HttpServletResponse.SC_FORBIDDEN,
                "{\"success\":false,\"message\":\"" + esc(message) + "\"}");
    }

    // escape text
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String path = req.getPathInfo();
        if (path == null) path = "";

        // room availability
        if ("/availability".equals(path)) {

            // access check
            if (!isStaffOrAdmin(req)) {
                denyAccess(resp, "Staff/Admin access required");
                return;
            }

            // read dates
            String checkInStr = req.getParameter("checkIn");
            String checkOutStr = req.getParameter("checkOut");

            if (checkInStr == null || checkOutStr == null || checkInStr.trim().isEmpty() || checkOutStr.trim().isEmpty()) {
                sendJson(resp, 400, "{\"success\":false,\"message\":\"checkIn and checkOut required\"}");
                return;
            }

            try {
                Date checkIn = Date.valueOf(checkInStr.trim());
                Date checkOut = Date.valueOf(checkOutStr.trim());

                String json = reservationService.listRoomsWithAvailabilityJson(checkIn, checkOut);
                sendJson(resp, 200, json);
                return;

            } catch (Exception e) {
                sendJson(resp, 400, "{\"success\":false,\"message\":\"Invalid date format (use YYYY-MM-DD)\"}");
                return;
            }
        }

        // admin access for room management
        if (!isAdmin(req)) {
            denyAccess(resp, "Admin access required");
            return;
        }

        // room detail
        if ("/detail".equals(path)) {
            String idStr = req.getParameter("id");
            if (idStr == null || idStr.trim().isEmpty()) {
                sendJson(resp, 400, "{\"success\":false,\"message\":\"id required\"}");
                return;
            }

            try {
                int id = Integer.parseInt(idStr.trim());
                Room r = roomService.getRoom(id);
                if (r == null) {
                    sendJson(resp, 404, "{\"success\":false,\"message\":\"Room not found\"}");
                    return;
                }

                sendJson(resp, 200,
                        "{\"success\":true,\"room\":{" +
                                "\"roomId\":" + r.getRoomId() + "," +
                                "\"roomNumber\":\"" + esc(r.getRoomNumber()) + "\"," +
                                "\"roomType\":\"" + esc(r.getRoomType()) + "\"," +
                                "\"price\":" + r.getRatePerNight() + "," +
                                "\"maxGuests\":" + r.getMaxGuests() + "," +
                                "\"status\":\"" + esc(r.getStatus()) + "\"," +
                                "\"description\":\"" + esc(r.getDescription()) + "\"," +
                                "\"imageUrl\":\"" + esc(r.getImageUrl()) + "\"" +
                                "}}"
                );
                return;

            } catch (NumberFormatException e) {
                sendJson(resp, 400, "{\"success\":false,\"message\":\"Invalid id\"}");
                return;
            }
        }

        // list rooms
        try {
            List<Room> rooms = roomService.listRooms();
            StringBuilder sb = new StringBuilder("{\"success\":true,\"rooms\":[");

            for (int i = 0; i < rooms.size(); i++) {
                Room r = rooms.get(i);

                sb.append("{")
                        .append("\"roomId\":").append(r.getRoomId()).append(",")
                        .append("\"roomNumber\":\"").append(esc(r.getRoomNumber())).append("\",")
                        .append("\"roomType\":\"").append(esc(r.getRoomType())).append("\",")
                        .append("\"price\":").append(r.getRatePerNight()).append(",")
                        .append("\"maxGuests\":").append(r.getMaxGuests()).append(",")
                        .append("\"status\":\"").append(esc(r.getStatus())).append("\",")
                        .append("\"description\":\"").append(esc(r.getDescription())).append("\",")
                        .append("\"imageUrl\":\"").append(esc(r.getImageUrl())).append("\"")
                        .append("}");

                if (i < rooms.size() - 1) sb.append(",");
            }

            sb.append("]}");
            sendJson(resp, 200, sb.toString());
        } catch (Exception e) {
            sendJson(resp, 500, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // admin access
        if (!isAdmin(req)) {
            denyAccess(resp, "Admin access required");
            return;
        }

        String body = getBody(req);

        try {
            String roomNumber = extract(body, "roomNumber", "").trim();
            String roomType = extract(body, "roomType", "").trim();
            double price = Double.parseDouble(extract(body, "price", "0"));
            int maxGuests = Integer.parseInt(extract(body, "maxGuests", "2"));
            String status = extract(body, "status", "AVAILABLE").trim();

            String description = extract(body, "description", "").trim();
            String imageUrl = extract(body, "imageUrl", "").trim();

            if (roomNumber.isEmpty()) throw new IllegalArgumentException("Room number required");
            if (roomType.isEmpty()) throw new IllegalArgumentException("Room type required");

            int newId = roomService.createRoom(roomNumber, roomType, price, status, maxGuests, description, imageUrl);
            sendJson(resp, 201, "{\"success\":true,\"roomId\":" + newId + "}");
        } catch (NumberFormatException e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"Invalid number format\"}");
        } catch (Exception e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // admin access
        if (!isAdmin(req)) {
            denyAccess(resp, "Admin access required");
            return;
        }

        String body = getBody(req);

        try {
            String roomIdStr = extract(body, "roomId", "").trim();
            if (roomIdStr.isEmpty() || "null".equalsIgnoreCase(roomIdStr) || "0".equals(roomIdStr)) {
                sendJson(resp, 400, "{\"success\":false,\"message\":\"Room ID required for update\"}");
                return;
            }

            int id = Integer.parseInt(roomIdStr);

            String roomNumber = extract(body, "roomNumber", "").trim();
            String roomType = extract(body, "roomType", "").trim();
            double price = Double.parseDouble(extract(body, "price", "0"));
            int maxGuests = Integer.parseInt(extract(body, "maxGuests", "2"));
            String status = extract(body, "status", "AVAILABLE").trim();

            String description = extract(body, "description", "").trim();
            String imageUrl = extract(body, "imageUrl", "").trim();

            if (roomNumber.isEmpty()) throw new IllegalArgumentException("Room number required");
            if (roomType.isEmpty()) throw new IllegalArgumentException("Room type required");

            boolean ok = roomService.updateRoom(id, roomNumber, roomType, price, status, maxGuests, description, imageUrl);

            if (ok) sendJson(resp, 200, "{\"success\":true}");
            else sendJson(resp, 404, "{\"success\":false,\"message\":\"Room not found\"}");
        } catch (NumberFormatException e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"Invalid number format\"}");
        } catch (Exception e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // admin access
        if (!isAdmin(req)) {
            denyAccess(resp, "Admin access required");
            return;
        }

        String idStr = req.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"ID required\"}");
            return;
        }

        try {
            int id = Integer.parseInt(idStr.trim());
            boolean ok = roomService.deleteRoom(id);
            if (ok) sendJson(resp, 200, "{\"success\":true}");
            else sendJson(resp, 404, "{\"success\":false,\"message\":\"Room not found\"}");
        } catch (NumberFormatException e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"Invalid ID\"}");
        }
    }

    // read request body
    private String getBody(HttpServletRequest req) throws IOException {
        java.util.Scanner s = new java.util.Scanner(req.getInputStream()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    // extract value from json string
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
