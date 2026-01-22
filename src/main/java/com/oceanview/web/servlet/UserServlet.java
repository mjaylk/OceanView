package com.oceanview.web.servlet;

import com.oceanview.model.User;
import com.oceanview.service.UserService;
import com.oceanview.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

@WebServlet("/api/users")
public class UserServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private UserService userService;

    @Override
    public void init() {
        userService = new UserService();
        System.out.println("TEST CASE 0: UserServlet initialized");
    }

    // --------------------------
    // AUTHORIZATION (ADMIN ONLY)
    // --------------------------
    private boolean isAdmin(HttpServletRequest req) {
        HttpSession session = req.getSession(false);

        if (session == null) {
            System.out.println("TEST CASE 1: No session found");
            return false;
        }

        User user = (User) session.getAttribute("user");
        if (user == null) {
            System.out.println("TEST CASE 2: No user in session");
            return false;
        }

        boolean admin = "ADMIN".equalsIgnoreCase(user.getRole());
        System.out.println("TEST CASE 3: user=" + user.getUsername() + ", role=" + user.getRole() + ", isAdmin=" + admin);
        return admin;
    }

    private void sendJson(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(json);
    }

    private void denyAdminOnly(HttpServletResponse resp) throws IOException {
        System.out.println("TEST CASE X: Access denied (Admin only)");
        sendJson(resp, HttpServletResponse.SC_FORBIDDEN,
                "{\"success\":false,\"message\":\"Admin only\"}");
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try { return Integer.parseInt(((String) v).trim()); }
            catch (Exception ignored) { return null; }
        }
        return null;
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // --------------------------
    // GET /api/users  (List users)
    // --------------------------
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        System.out.println("TEST CASE 4: GET /api/users called");

        if (!isAdmin(req)) {
            denyAdminOnly(resp);
            return;
        }

        List<User> users = userService.listUsers();
        System.out.println("TEST CASE 5: Users count=" + users.size());

        // Build JSON output (do not expose password hash)
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

    // --------------------------
    // POST /api/users  (Create user)
    // --------------------------
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        System.out.println("TEST CASE 11: POST /api/users called");

        if (!isAdmin(req)) {
            System.out.println("TEST CASE 12: POST denied - not admin");
            denyAdminOnly(resp);
            return;
        }

        Map<String, Object> body = JsonUtil.readJson(req);

        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String role     = (String) body.get("role");

        System.out.println("TEST CASE 13: Create user username=" + username + ", role=" + role);

        try {
            int id = userService.createUser(username, password, role);
            System.out.println("TEST CASE 14: Created userId=" + id);

            sendJson(resp, HttpServletResponse.SC_CREATED,
                    "{\"success\":true,\"userId\":" + id + "}");

        } catch (IllegalArgumentException ex) {
            System.out.println("TEST CASE 15: Create failed: " + ex.getMessage());

            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"" + esc(ex.getMessage()) + "\"}");
        }
    }

    // --------------------------
    // PUT /api/users  (Update user + optional reset password)
    // --------------------------
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        System.out.println("TEST CASE 16: PUT /api/users called");

        if (!isAdmin(req)) {
            System.out.println("TEST CASE 16A: PUT denied - not admin");
            denyAdminOnly(resp);
            return;
        }

        Map<String, Object> body = JsonUtil.readJson(req);

        Integer userId = toInt(body.get("userId"));
        String username = (String) body.get("username");
        String role     = (String) body.get("role");
        String status   = (String) body.get("status");
        String newPassword = (String) body.get("newPassword");

        System.out.println("TEST CASE 17: Update request userId=" + userId);

        if (userId == null) {
            System.out.println("TEST CASE 17A: Invalid userId");
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"userId is required\"}");
            return;
        }

        try {
            userService.updateUser(userId, username, role, status);

            if (newPassword != null && !newPassword.isBlank()) {
                userService.resetPassword(userId, newPassword);
                System.out.println("TEST CASE 18: Password reset done for userId=" + userId);
            }

            System.out.println("TEST CASE 19: Update success");
            sendJson(resp, HttpServletResponse.SC_OK,
                    "{\"success\":true}");

        } catch (IllegalArgumentException ex) {
            System.out.println("TEST CASE 20: Update failed: " + ex.getMessage());

            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"" + esc(ex.getMessage()) + "\"}");
        }
    }

    // --------------------------
    // DELETE /api/users?id=5  (Deactivate user)
    // --------------------------
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        System.out.println("TEST CASE 21: DELETE /api/users called");

        if (!isAdmin(req)) {
            System.out.println("TEST CASE 21A: DELETE denied - not admin");
            denyAdminOnly(resp);
            return;
        }

        String idStr = req.getParameter("id");
        System.out.println("TEST CASE 22: Deactivate id param=" + idStr);

        Integer id = null;
        try { id = Integer.parseInt(idStr); } catch (Exception ignored) {}

        if (id == null) {
            System.out.println("TEST CASE 22A: Invalid id parameter");
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"Valid id query param required\"}");
            return;
        }

        boolean ok = userService.deactivateUser(id);
        System.out.println("TEST CASE 23: Deactivate result=" + ok);

        sendJson(resp, HttpServletResponse.SC_OK,
                "{\"success\":" + ok + "}");
    }
}
