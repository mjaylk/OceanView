package com.oceanview.web.servlet;

import com.oceanview.model.User;
import com.oceanview.service.UserService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/test-users")
public class TestUsersServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
	private UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || !"ADMIN".equalsIgnoreCase(((User)session.getAttribute("user")).getRole())) {
            resp.sendError(403, "Admin only");
            return;
        }

        List<User> users = userService.listUsers();
        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html><html><head><title>Users</title></head><body>");
        out.println("<h2>All Users (" + users.size() + ")</h2>");
        out.println("<table border='1'><tr><th>ID</th><th>Username</th><th>Role</th><th>Status</th></tr>");
        for (User u : users) {
            out.println("<tr><td>" + u.getUserId() + "</td><td>" + u.getUsername() 
                      + "</td><td>" + u.getRole() + "</td><td>" + u.getStatus() + "</td></tr>");
        }
        out.println("</table></body></html>");
    }
}
