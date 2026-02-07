package com.oceanview.web.servlet;

import com.oceanview.model.User;
import com.oceanview.service.UserService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet("/api/users")
public class UserServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // service object
    private UserService userService;

    @Override
    public void init() {
        userService = new UserService();
    }

    // admin check
    private boolean isAdmin(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return false;
        User user = (User) session.getAttribute("user");
        if (user == null) return false;
        return "ADMIN".equalsIgnoreCase(user.getRole());
    }

    // send json response
    private void sendJson(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(json);
    }

    // deny if not admin
    private void denyAdminOnly(HttpServletResponse resp) throws IOException {
        sendJson(resp, HttpServletResponse.SC_FORBIDDEN,
                "{\"success\":false,\"message\":\"Admin only\"}");
    }

    // convert to int
    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try {
                return Integer.parseInt(((String) v).trim());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    // escape text
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // read json body
    private Map<String, Object> readJsonBody(HttpServletRequest req) throws IOException {
        String contentType = req.getContentType();
        if (contentType == null || !contentType.contains("application/json")) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = req.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        String json = sb.toString().trim();
        if (json.isEmpty()) return null;

        Map<String, Object> map = new HashMap<>();

        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"([^\"]*)\"|(\\d+))");
        Matcher matcher = pattern.matcher(json);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(3) != null ? matcher.group(3) : matcher.group(4);
            if ("userId".equals(key)) {
                map.put(key, toInt(value));
            } else {
                map.put(key, value);
            }
        }

        return map.isEmpty() ? null : map;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // admin access
        if (!isAdmin(req)) {
            denyAdminOnly(resp);
            return;
        }

        // list users
        List<User> users = userService.listUsers();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\":true,\"users\":[");
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            sb.append("{")
                    .append("\"userId\":").append(u.getUserId()).append(",")
                    .append("\"username\":\"").append(esc(u.getUsername())).append("\",")
                    .append("\"role\":\"").append(esc(u.getRole())).append("\",")
                    .append("\"status\":\"").append(esc(u.getStatus())).append("\"")
                    .append("}");
            if (i < users.size() - 1) sb.append(",");
        }
        sb.append("]}");
        sendJson(resp, HttpServletResponse.SC_OK, sb.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // admin access
        if (!isAdmin(req)) {
            denyAdminOnly(resp);
            return;
        }

        Map<String, Object> body = readJsonBody(req);
        if (body == null) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"Invalid JSON\"}");
            return;
        }

        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String role = (String) body.get("role");

        try {
            int id = userService.createUser(username, password, role);
            sendJson(resp, HttpServletResponse.SC_CREATED,
                    "{\"success\":true,\"userId\":" + id + "}");
        } catch (IllegalArgumentException ex) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"" + esc(ex.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // admin access
        if (!isAdmin(req)) {
            denyAdminOnly(resp);
            return;
        }

        Map<String, Object> body = readJsonBody(req);
        if (body == null) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"Invalid JSON\"}");
            return;
        }

        Integer userId = toInt(body.get("userId"));
        String username = (String) body.get("username");
        String role = (String) body.get("role");
        String status = (String) body.get("status");
        String newPassword = (String) body.get("newPassword");

        // user id check
        if (userId == null) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"userId required\"}");
            return;
        }

        try {
            userService.updateUser(userId, username, role, status);

            // reset password
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                userService.resetPassword(userId, newPassword);
            }

            sendJson(resp, HttpServletResponse.SC_OK, "{\"success\":true}");
        } catch (IllegalArgumentException ex) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"" + esc(ex.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // admin access
        if (!isAdmin(req)) {
            denyAdminOnly(resp);
            return;
        }

        // read id
        String idStr = req.getParameter("id");
        Integer id = null;
        try {
            id = Integer.parseInt(idStr);
        } catch (Exception ignored) {
        }

        if (id == null) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"Valid id required\"}");
            return;
        }

        boolean ok = userService.deleteUser(id);
        sendJson(resp, HttpServletResponse.SC_OK, "{\"success\":" + ok + "}");
    }
}
