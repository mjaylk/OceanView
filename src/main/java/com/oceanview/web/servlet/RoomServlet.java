package com.oceanview.web.servlet;

import com.oceanview.model.Room;
import com.oceanview.service.RoomService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/api/rooms/*")
public class RoomServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private RoomService roomService;

    @Override
    public void init() {
        roomService = new RoomService();
    }

    private boolean isAdmin(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return false;
        Object user = session.getAttribute("user");
        if (user == null) return false;
        return "ADMIN".equalsIgnoreCase(((com.oceanview.model.User) user).getRole());
    }

    private void sendJson(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(json);
    }

    private void denyAccess(HttpServletResponse resp) throws IOException {
        sendJson(resp, HttpServletResponse.SC_FORBIDDEN,
                "{\"success\":false,\"message\":\"Admin access required\"}");
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
        if (!isAdmin(req)) {
            denyAccess(resp);
            return;
        }

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
                        .append("\"description\":\"\"")
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
        if (!isAdmin(req)) {
            denyAccess(resp);
            return;
        }

        String body = getBody(req);

        try {
            String roomNumber = extract(body, "roomNumber");
            String roomType = extract(body, "roomType");
            double price = Double.parseDouble(extract(body, "price", "0"));
            int maxGuests = Integer.parseInt(extract(body, "maxGuests", "2"));
            String status = extract(body, "status", "AVAILABLE");

            int newId = roomService.createRoom(roomNumber, roomType, price, status, maxGuests);

            sendJson(resp, 201, "{\"success\":true,\"roomId\":" + newId + "}");
        } catch (Exception e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!isAdmin(req)) {
            denyAccess(resp);
            return;
        }

        String body = getBody(req);

        try {
            String roomIdStr = extract(body, "roomId", "");
            if (roomIdStr.trim().isEmpty() || "null".equalsIgnoreCase(roomIdStr) || "0".equals(roomIdStr)) {
                sendJson(resp, 400, "{\"success\":false,\"message\":\"Room ID required for update\"}");
                return;
            }

            int id = Integer.parseInt(roomIdStr);

            String roomNumber = extract(body, "roomNumber");
            String roomType = extract(body, "roomType");
            double price = Double.parseDouble(extract(body, "price", "0"));
            int maxGuests = Integer.parseInt(extract(body, "maxGuests", "2"));
            String status = extract(body, "status", "AVAILABLE");

            boolean ok = roomService.updateRoom(id, roomNumber, roomType, price, status, maxGuests);

            if (ok) {
                sendJson(resp, 200, "{\"success\":true}");
            } else {
                sendJson(resp, 404, "{\"success\":false,\"message\":\"Room not found\"}");
            }
        } catch (NumberFormatException e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"Invalid number format\"}");
        } catch (Exception e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!isAdmin(req)) {
            denyAccess(resp);
            return;
        }

        String idStr = req.getParameter("id");
        if (idStr == null) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"ID required\"}");
            return;
        }

        try {
            int id = Integer.parseInt(idStr);
            boolean ok = roomService.deleteRoom(id);
            if (ok) sendJson(resp, 200, "{\"success\":true}");
            else sendJson(resp, 404, "{\"success\":false,\"message\":\"Room not found\"}");
        } catch (NumberFormatException e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"Invalid ID\"}");
        }
    }

    private String getBody(HttpServletRequest req) throws IOException {
        java.util.Scanner s = new java.util.Scanner(req.getInputStream()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private String extract(String json, String key) {
        return extract(json, key, "");
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
