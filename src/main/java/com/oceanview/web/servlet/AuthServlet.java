package com.oceanview.web.servlet;

import com.oceanview.model.User;
import com.oceanview.service.AuthService;
import com.oceanview.util.Flash;

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

        System.out.println("\n========== LOGIN ATTEMPT ==========");
        
        req.setCharacterEncoding("UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        System.out.println("Username: " + username);
        System.out.println("Password: " + (password != null ? "***" : "null"));

        User user = authService.login(username, password);

        if (user == null) {
            System.out.println("LOGIN FAILED");
            
            HttpSession session = req.getSession(true);
            System.out.println("Session ID: " + session.getId());
            System.out.println("Session is new: " + session.isNew());
            
            Flash.error(req, "Invalid username or password. Please try again.");
            
            System.out.println("Flash error set. Checking session attribute...");
            Object errorInSession = session.getAttribute(Flash.KEY_ERROR);
            System.out.println("Error in session after Flash.error(): " + errorInSession);
            
            String redirectUrl = req.getContextPath() + "/login.html";
            System.out.println("Redirecting to: " + redirectUrl);
            System.out.println("===================================\n");
            
            resp.sendRedirect(redirectUrl);
            return;
        }

        System.out.println("LOGIN SUCCESSFUL for user: " + user.getUsername());
        
        HttpSession session = req.getSession(true);
        System.out.println("Session ID: " + session.getId());
        
        session.setAttribute("user", user);
        session.setMaxInactiveInterval(60 * 60);

        Cookie userCookie = new Cookie("oceanview_user", user.getUsername());
        userCookie.setPath(req.getContextPath());
        userCookie.setMaxAge(3600);
        resp.addCookie(userCookie);

        String ctxPath = req.getContextPath();
        String role = user.getRole() == null ? "" : user.getRole().toUpperCase();

        String redirectUrl;
        if ("ADMIN".equals(role) || "STAFF".equals(role)) {
            Flash.success(req, "Login successful. Welcome back, " + user.getUsername() + ".");
            redirectUrl = ctxPath + "/dashboard.html";
        } else {
            Flash.success(req, "Login successful.");
            redirectUrl = ctxPath + "/guest.html";
        }
        
        System.out.println("Redirecting to: " + redirectUrl);
        System.out.println("===================================\n");
        
        resp.sendRedirect(redirectUrl);
    }
}