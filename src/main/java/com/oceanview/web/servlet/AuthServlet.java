package com.oceanview.web.servlet;

import com.oceanview.model.User;
import com.oceanview.service.AuthService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/api/login")
public class AuthServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // service object
    private final AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // request encoding
        req.setCharacterEncoding("UTF-8");

        // read inputs
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // login check
        User user = authService.login(username, password);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login.html?error=1");
            return;
        }

        // create session
        HttpSession session = req.getSession(true);
        session.setAttribute("user", user);
        session.setMaxInactiveInterval(60 * 60);

        // add cookie
        Cookie userCookie = new Cookie("oceanview_user", user.getUsername());
        userCookie.setPath(req.getContextPath());
        userCookie.setMaxAge(3600);
        resp.addCookie(userCookie);

        // role redirect
        String ctxPath = req.getContextPath();
        String role = user.getRole() == null ? "" : user.getRole().toUpperCase();

        if ("ADMIN".equals(role) || "STAFF".equals(role)) {
            resp.sendRedirect(ctxPath + "/dashboard.html");
        } else {
            resp.sendRedirect(ctxPath + "/guest.html");
        }
    }
}
