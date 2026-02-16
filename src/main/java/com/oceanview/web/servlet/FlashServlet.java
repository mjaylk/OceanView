package com.oceanview.web.servlet;

import com.oceanview.util.Flash;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/api/flash")
public class FlashServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);

        String success = "";
        String error = "";

        if (session != null) {
            Object s = session.getAttribute(Flash.KEY_SUCCESS);
            Object e = session.getAttribute(Flash.KEY_ERROR);

            if (s != null) {
                success = String.valueOf(s);
                // show only once
                session.removeAttribute(Flash.KEY_SUCCESS);
            }
            
            if (e != null) {
                error = String.valueOf(e);
                // show only once 
                session.removeAttribute(Flash.KEY_ERROR);
            }
            
       
            if (!error.isEmpty()) {
                success = ""; // Clear success if there's an error
            }
        }

        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Expires", "0");
        resp.getWriter().write("{\"success\":\"" + esc(success) + "\",\"error\":\"" + esc(error) + "\"}");
    }

    private String esc(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}