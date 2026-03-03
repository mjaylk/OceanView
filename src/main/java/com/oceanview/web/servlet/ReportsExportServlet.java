package com.oceanview.web.servlet;

import com.oceanview.model.Reservation;
import com.oceanview.model.User;
import com.oceanview.service.ReservationService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.util.List;

@WebServlet("/api/reports/export")
public class ReportsExportServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
	private ReservationService service;

    @Override
    public void init() {
        service = new ReservationService();
    }

    private boolean isStaffOrAdmin(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return false;
        User user = (User) session.getAttribute("user");
        if (user == null) return false;
        String role = user.getRole();
        return "ADMIN".equalsIgnoreCase(role) || "STAFF".equalsIgnoreCase(role);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        if (!isStaffOrAdmin(req)) {
            resp.sendError(403, "Access denied");
            return;
        }

        String from = req.getParameter("from");
        String to   = req.getParameter("to");

        List<Reservation> list;

        try {
            if (from != null && !from.isEmpty() && to != null && !to.isEmpty()) {
                list = service.getBetween(Date.valueOf(from), Date.valueOf(to));
            } else {
                list = service.listReservations();
            }
        } catch (Exception e) {
            resp.sendError(400, "Invalid date range");
            return;
        }

        // Set CSV response headers
        resp.setContentType("text/csv;charset=UTF-8");
        resp.setHeader("Content-Disposition",
                "attachment; filename=\"oceanview-report-" + (from != null ? from : "all") + ".csv\"");

        PrintWriter pw = resp.getWriter();

        // CSV Header
        pw.println("Reservation No,Guest Name,Guest Email,Guest Phone,Room Number,Room Type,Check-In,Check-Out,Nights,Status,Total Amount,Amount Paid,Payment Status");

        // CSV Rows
        for (Reservation r : list) {
            pw.println(
                csv(r.getReservationNumber()) + "," +
                csv(r.getGuestName())         + "," +
                csv(r.getGuestEmail())        + "," +
                csv(r.getGuestContactNumber())+ "," +
                csv(r.getRoomNumber())        + "," +
                csv(r.getRoomType())          + "," +
                csv(String.valueOf(r.getCheckInDate()))  + "," +
                csv(String.valueOf(r.getCheckOutDate())) + "," +
                r.getNights()                 + "," +
                csv(r.getStatus())            + "," +
                r.getTotalAmount()            + "," +
                r.getAmountPaid()             + "," +
                csv(r.getPaymentStatus())
            );
        }

        pw.flush();
    }

    // Escape CSV values
    private String csv(String val) {
        if (val == null) return "";
   
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
