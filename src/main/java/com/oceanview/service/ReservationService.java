package com.oceanview.service;

import com.oceanview.dao.ReservationDAO;
import com.oceanview.dao.impl.ReservationDAOImpl;
import com.oceanview.model.Reservation;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;

public class ReservationService {
    private final ReservationDAO dao = new ReservationDAOImpl();
    private final GuestService guestService = new GuestService();

    public List<Reservation> listReservations() {
        return dao.findAll();
    }

    public Reservation getById(int id) {
        return dao.findById(id);
    }

    public Reservation getByNumber(String number) {
        if (number == null || number.trim().isEmpty()) return null;
        return dao.findByNumber(number.trim());
    }

    public List<Reservation> getBookedByRoom(int roomId) {
        if (roomId <= 0) throw new IllegalArgumentException("Invalid roomId");
        return dao.findByRoom(roomId);
    }

    public List<Reservation> getBetween(Date start, Date end) {
        if (start == null || end == null) throw new IllegalArgumentException("Start/end required");
        return dao.findBetween(start, end);
    }

    public int createReservationSmart(
            int guestId,
            String guestName,
            String guestEmail,
            String guestContactNumber,
            int roomId,
            Date checkIn,
            Date checkOut,
            String status,
            int createdBy
    ) {
        // ---- server validation ----
        if (roomId <= 0) throw new IllegalArgumentException("Room is required");
        if (checkIn == null || checkOut == null) throw new IllegalArgumentException("Check-in/out required");
        if (checkOut.compareTo(checkIn) <= 0) throw new IllegalArgumentException("Check-out must be after check-in");
        if (createdBy <= 0) throw new IllegalArgumentException("Session expired. Please login again.");

        if (status == null || status.trim().isEmpty()) status = "PENDING";
        status = status.trim().toUpperCase();

        if (!status.equals("PENDING") && !status.equals("CONFIRMED") && !status.equals("CHECKED_IN")) {
            throw new IllegalArgumentException("Invalid status");
        }

        // ---- ensure guest exists ----
        if (guestId <= 0) {
            guestId = guestService.ensureGuest(guestName, guestEmail, guestContactNumber);
        }

        // ---- overlap check ----
        boolean overlap = dao.hasOverlappingReservation(roomId, checkIn, checkOut);
        if (overlap) {
            throw new IllegalArgumentException("Selected dates overlap with an existing reservation.");
        }

        // ---- create reservation number ----
        String number = generateReservationNumber(checkIn);

        Reservation r = new Reservation();
        r.setReservationNumber(number);
        r.setGuestId(guestId);
        r.setRoomId(roomId);
        r.setCheckInDate(checkIn);
        r.setCheckOutDate(checkOut);
        r.setStatus(status);
        r.setCreatedBy(createdBy);

        int id = dao.create(r);
        if (id <= 0) throw new IllegalStateException("Reservation insert failed");
        return id;
    }

    private String generateReservationNumber(Date checkIn) {
        // prefix based on date: RES-YYYYMMDD-
        String yyyymmdd = new SimpleDateFormat("yyyyMMdd").format(checkIn);
        String prefix = "RES-" + yyyymmdd + "-";

        String last = dao.findLastReservationNumberForDate(prefix);
        int next = 1;

        if (last != null && last.startsWith(prefix)) {
            String tail = last.substring(prefix.length()); // e.g. "007"
            try {
                next = Integer.parseInt(tail) + 1;
            } catch (Exception ignored) {
                next = 1;
            }
        }

        return prefix + String.format("%03d", next);
    }
}
