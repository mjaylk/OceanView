package com.oceanview.web.servlet;

import com.oceanview.model.User;
import com.oceanview.service.AuthService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/api/login")
public class AuthServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        System.out.println("AuthServlet doPost called");

        req.setCharacterEncoding("UTF-8");
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        System.out.println("Login attempt: " + username);

        User user = authService.login(username, password);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login.html?error=1");
            return;
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("user", user);
        System.out.println("Session created for " + user.getUsername() + " (Role: " + user.getRole() + ")");

        String ctxPath = req.getContextPath();
        String role = user.getRole().toUpperCase();

        if ("ADMIN".equals(role)) {
            resp.sendRedirect(ctxPath + "/admin.html");
        } else if ("STAFF".equals(role)) {
            resp.sendRedirect(ctxPath + "/staff.html");
        } else {
            resp.sendRedirect(ctxPath + "/guest.html");
        }
    }
}
