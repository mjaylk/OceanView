package com.oceanview.web.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/api/logout")
public class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // get session
        HttpSession session = req.getSession(false);

        // invalidate session
        if (session != null) {
            session.invalidate();
        }

        // clear admin session 
        Cookie sessionCookie = new Cookie("admin_session", "");
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(0);
        resp.addCookie(sessionCookie);

        // clear user cookie
        Cookie userCookie = new Cookie("oceanview_user", "");
        userCookie.setPath("/");
        userCookie.setMaxAge(0);
        resp.addCookie(userCookie);

        // frontend helper header
        resp.addHeader("X-Clear-SessionStorage", "true");

        // redirect to login
        resp.sendRedirect(req.getContextPath() + "/login.html");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // same as get
        doGet(req, resp);
    }
}
