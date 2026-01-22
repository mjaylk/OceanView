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

        System.out.println(">>> AuthServlet running");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        User user = authService.login(username, password);

        //  Login failed → back to login
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login.html?error=1");
            return;
        }

        // Login success → store user in session
        HttpSession session = req.getSession(true);
        session.setAttribute("user", user);

        // ROLE-WISE REDIRECT
        String role = user.getRole();

        if ("ADMIN".equalsIgnoreCase(role)) {
            resp.sendRedirect(req.getContextPath() + "/admin.html");
        } 
        else if ("STAFF".equalsIgnoreCase(role)) {
            resp.sendRedirect(req.getContextPath() + "/staff.html");
        } 
        else if ("GUEST".equalsIgnoreCase(role)) {
            resp.sendRedirect(req.getContextPath() + "/guest.html");
        } 
        else {
            // fallback safety
            resp.sendRedirect(req.getContextPath() + "/login.html");
        }
    }
}
