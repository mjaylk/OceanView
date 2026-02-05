package com.oceanview.web.servlet;

import com.oceanview.model.Guest;
import com.oceanview.model.Reservation;
import com.oceanview.service.ReservationService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/api/guest/reservations")
public class GuestReservationsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final ReservationService reservationService = new ReservationService();

    private void sendJson(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(json);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        HttpSession session = req.getSession(false);
        Guest guest = (session != null) ? (Guest) session.getAttribute("guest") : null;

        if (guest == null) {
            sendJson(resp, 401, "{\"success\":false,\"message\":\"Guest login required\"}");
            return;
        }

        List<Reservation> list = reservationService.listReservationsByGuest(guest.getGuestId());

        StringBuilder sb = new StringBuilder("{\"success\":true,\"reservations\":[");
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);

            sb.append("{")
                    .append("\"reservationId\":").append(r.getReservationId()).append(",")
                    .append("\"reservationNumber\":\"").append(esc(r.getReservationNumber())).append("\",")
                    .append("\"checkInDate\":\"").append(r.getCheckInDate()).append("\",")
                    .append("\"checkOutDate\":\"").append(r.getCheckOutDate()).append("\",")
                    .append("\"status\":\"").append(esc(r.getStatus())).append("\",")
                    .append("\"roomNumber\":\"").append(esc(r.getRoomNumber())).append("\",")
                    .append("\"roomType\":\"").append(esc(r.getRoomType())).append("\",")
                    .append("\"totalAmount\":").append(r.getTotalAmount())
                    .append("}");

            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]}");

        sendJson(resp, 200, sb.toString());
    }
}
