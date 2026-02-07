package com.oceanview.web.servlet;

import com.oceanview.model.ReservationPayment;
import com.oceanview.model.User;
import com.oceanview.service.ReservationPaymentService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/api/payments/*")
public class ReservationPaymentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // service object
    private ReservationPaymentService service;

    @Override
    public void init() {
        service = new ReservationPaymentService();
    }

    // staff or admin check
    private boolean isStaffOrAdmin(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return false;
        User user = (User) session.getAttribute("user");
        if (user == null) return false;
        String role = user.getRole();
        return "ADMIN".equalsIgnoreCase(role) || "STAFF".equalsIgnoreCase(role);
    }

    // get user from session
    private User sessionUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object u = session.getAttribute("user");
        return (u instanceof User) ? (User) u : null;
    }

    // send json response
    private void sendJson(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(json);
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

        // access check
        if (!isStaffOrAdmin(req)) {
            sendJson(resp, 403, "{\"success\":false,\"message\":\"Staff/Admin access required\"}");
            return;
        }

        String path = req.getPathInfo();
        if (path == null) path = "";

        // payment history
        if ("/history".equals(path)) {
            String ridStr = req.getParameter("reservationId");
            if (ridStr == null || ridStr.trim().isEmpty()) {
                sendJson(resp, 400, "{\"success\":false,\"message\":\"reservationId required\"}");
                return;
            }

            int reservationId = Integer.parseInt(ridStr);
            List<ReservationPayment> payments = service.getPaymentHistory(reservationId);

            StringBuilder sb = new StringBuilder("{\"success\":true,\"payments\":[");
            for (int i = 0; i < payments.size(); i++) {
                ReservationPayment p = payments.get(i);
                sb.append("{")
                        .append("\"paymentId\":").append(p.getPaymentId()).append(",")
                        .append("\"reservationId\":").append(p.getReservationId()).append(",")
                        .append("\"paidAmount\":").append(p.getPaidAmount()).append(",")
                        .append("\"paidDate\":\"").append(p.getPaidDate()).append("\",")
                        .append("\"method\":\"").append(esc(p.getMethod())).append("\",")
                        .append("\"note\":\"").append(esc(p.getNote())).append("\"")
                        .append("}");
                if (i < payments.size() - 1) sb.append(",");
            }
            sb.append("]}");
            sendJson(resp, 200, sb.toString());
            return;
        }

        sendJson(resp, 404, "{\"success\":false,\"message\":\"Endpoint not found\"}");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // access check
        if (!isStaffOrAdmin(req)) {
            sendJson(resp, 403, "{\"success\":false,\"message\":\"Staff/Admin access required\"}");
            return;
        }

        // session check
        User user = sessionUser(req);
        if (user == null) {
            sendJson(resp, 401, "{\"success\":false,\"message\":\"Session expired. Please login again.\"}");
            return;
        }

        String body = getBody(req);

        try {
            int reservationId = Integer.parseInt(extract(body, "reservationId", "0"));
            double amount = parseDoubleSafe(extract(body, "amount", "0"));
            String method = extract(body, "method", "");
            String note = extract(body, "note", "");

            int paymentId = service.addPayment(reservationId, amount, method, note, user.getUserId());

            sendJson(resp, 201, "{\"success\":true,\"paymentId\":" + paymentId + "}");

        } catch (Exception e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // access check
        if (!isStaffOrAdmin(req)) {
            sendJson(resp, 403, "{\"success\":false,\"message\":\"Staff/Admin access required\"}");
            return;
        }

        // session check
        User user = sessionUser(req);
        if (user == null) {
            sendJson(resp, 401, "{\"success\":false,\"message\":\"Session expired. Please login again.\"}");
            return;
        }

        try {
            int paymentId = Integer.parseInt(req.getParameter("id"));
            boolean ok = service.deletePayment(paymentId);
            if (!ok) {
                sendJson(resp, 404, "{\"success\":false,\"message\":\"Payment not found\"}");
                return;
            }
            sendJson(resp, 200, "{\"success\":true}");
        } catch (Exception e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
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

    // parse double safe
    private double parseDoubleSafe(String v) {
        try {
            return Double.parseDouble(v);
        } catch (Exception e) {
            return 0.0;
        }
    }
}