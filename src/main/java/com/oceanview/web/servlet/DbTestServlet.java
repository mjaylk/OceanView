package com.oceanview.web.servlet;

import com.oceanview.util.DatabaseConnection;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;

@WebServlet("/api/db-test")
public class DbTestServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");

        try (Connection con = DatabaseConnection.getInstance().getConnection()) {
            resp.getWriter().println("DB Connected: " + con.getMetaData().getURL());
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().println("DB Connection Failed");
            e.printStackTrace(resp.getWriter());
        }
    }
}
