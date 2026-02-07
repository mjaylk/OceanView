package com.oceanview.web.servlet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/api/guest/logout")
public class GuestLogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // get session
        HttpSession s = req.getSession(false);

        // invalidate session
        if (s != null) s.invalidate();

        // redirect to login
        resp.sendRedirect(req.getContextPath() + "/guest-login.html");
    }
}
