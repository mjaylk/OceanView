package com.oceanview.web.servlet;

import com.oceanview.model.User;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/api/me")
public class MeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

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
        HttpSession session = req.getSession(false);
        if (session == null) {
            sendJson(resp, 401, "{\"success\":false,\"message\":\"No session\"}");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (user == null) {
            sendJson(resp, 401, "{\"success\":false,\"message\":\"Not logged in\"}");
            return;
        }

        sendJson(resp, 200,
                "{\"success\":true," +
                        "\"username\":\"" + esc(user.getUsername()) + "\"," +
                        "\"role\":\"" + esc(user.getRole()) + "\"}"
        );
    }
}
