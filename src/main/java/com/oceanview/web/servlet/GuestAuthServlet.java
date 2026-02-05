package com.oceanview.web.servlet;

import com.oceanview.model.Guest;
import com.oceanview.service.GuestService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/api/guest/login")
public class GuestAuthServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final GuestService guestService = new GuestService();

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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");

        // We read using getParameter because frontend sends x-www-form-urlencoded
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        if (email == null || email.trim().isEmpty()) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"Email required\"}");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"Password required\"}");
            return;
        }

        Guest g;
        try {
            g = guestService.loginGuest(email.trim(), password.trim());
        } catch (Exception ex) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"" + esc(ex.getMessage()) + "\"}");
            return;
        }

        if (g == null) {
            sendJson(resp, 401, "{\"success\":false,\"message\":\"Invalid email or password\"}");
            return;
        }

        // Create a session for the guest
        HttpSession session = req.getSession(true);
        session.setAttribute("guest", g);

        sendJson(resp, 200,
                "{\"success\":true,\"guestId\":" + g.getGuestId() +
                        ",\"fullName\":\"" + esc(g.getFullName()) + "\"" +
                        ",\"email\":\"" + esc(g.getEmail()) + "\"}"
        );
    }
}
