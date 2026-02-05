package com.oceanview.service;

import com.oceanview.dao.ReservationDAO;
import com.oceanview.dao.RoomDAO;
import com.oceanview.dao.impl.ReservationDAOImpl;
import com.oceanview.dao.impl.RoomDAOImpl;
import com.oceanview.model.Reservation;
import com.oceanview.util.EmailUtil;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;

public class ReservationService {

    private final ReservationDAO dao = new ReservationDAOImpl();
    private final GuestService guestService = new GuestService();
    private final RoomDAO roomDao = new RoomDAOImpl();

    private static final double DEFAULT_TAX_RATE = 0.0;

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

    public boolean deleteReservation(int id) {
        if (id <= 0) throw new IllegalArgumentException("Invalid reservation id");
        return dao.delete(id);
    }

    public List<Reservation> getRecentCheckins() {
        return dao.getRecentCheckins();
    }

    public List<Reservation> listReservationsByGuest(int guestId) {
        if (guestId <= 0) throw new IllegalArgumentException("Invalid guestId");
        return dao.findByGuestId(guestId);
    }

    
    public boolean updateReservationSmart(
            int reservationId,
            int guestId,
            String guestName,
            String guestEmail,
            String guestContactNumber,
            int roomId,
            Date checkIn,
            Date checkOut,
            String status,
            String notes,
            double taxRate,
            double discount,
            int updatedBy
    ) {
        if (reservationId <= 0) throw new IllegalArgumentException("Invalid reservation");
        if (roomId <= 0) throw new IllegalArgumentException("Room is required");
        if (checkIn == null || checkOut == null) throw new IllegalArgumentException("Check-in/out required");
        if (checkOut.compareTo(checkIn) <= 0) throw new IllegalArgumentException("Check-out must be after check-in");
        if (updatedBy <= 0) throw new IllegalArgumentException("Session expired. Please login again.");

        status = normalizeStatus(status);

        if (guestId <= 0) {
            guestId = guestService.ensureGuest(guestName, guestEmail, guestContactNumber);
        }

        Reservation existing = dao.findById(reservationId);
        if (existing == null) throw new IllegalArgumentException("Reservation not found");

        boolean changedRoomOrDates =
                existing.getRoomId() != roomId
                        || !existing.getCheckInDate().equals(checkIn)
                        || !existing.getCheckOutDate().equals(checkOut);

        if (changedRoomOrDates) {
            boolean overlap = dao.hasOverlappingReservationExceptSelf(roomId, reservationId, checkIn, checkOut);
            if (overlap) throw new IllegalArgumentException("Selected dates overlap with an existing reservation.");
        }

        int nights = calcNights(checkIn, checkOut);

        double rate = roomDao.findPriceById(roomId);
        if (rate <= 0) throw new IllegalArgumentException("Room price not found");

        if (taxRate < 0) taxRate = 0;
        if (discount < 0) discount = 0;

        double subtotal = round2(nights * rate);
        double tax = round2(subtotal * (taxRate / 100.0));
        double total = round2(subtotal + tax - discount);
        if (total < 0) total = 0;

        Reservation r = new Reservation();
        r.setReservationId(reservationId);
        r.setReservationNumber(existing.getReservationNumber());
        r.setGuestId(guestId);
        r.setRoomId(roomId);
        r.setCheckInDate(checkIn);
        r.setCheckOutDate(checkOut);
        r.setStatus(status);
        r.setNotes(notes);
        r.setCreatedBy(existing.getCreatedBy());

        r.setNights(nights);
        r.setRatePerNight(rate);
        r.setSubtotal(subtotal);
        r.setTax(tax);
        r.setDiscount(discount);
        r.setTotalAmount(total);

        return dao.update(r);
    }

    public int createReservationSmart(
            int guestId,
            String guestName,
            String guestEmail,
            String guestContactNumber,
            String guestPassword,
            int roomId,
            Date checkIn,
            Date checkOut,
            String status,
            String notes,
            Double taxRate,
            Double discount,
            int createdBy
    ) {
        if (roomId <= 0) throw new IllegalArgumentException("Room is required");
        if (checkIn == null || checkOut == null) throw new IllegalArgumentException("Check-in/out required");
        if (checkOut.compareTo(checkIn) <= 0) throw new IllegalArgumentException("Check-out must be after check-in");
        if (createdBy <= 0) throw new IllegalArgumentException("Session expired. Please login again.");

        status = normalizeStatus(status);

        double taxRateVal = (taxRate == null) ? DEFAULT_TAX_RATE : taxRate;
        double discountVal = (discount == null) ? 0.0 : discount;

        if (taxRateVal < 0) taxRateVal = 0;
        if (discountVal < 0) discountVal = 0;

        String passwordForEmail = null;

    
        if (guestId <= 0) {
            if (guestEmail == null || guestEmail.trim().isEmpty()) {
                throw new IllegalArgumentException("Guest email required to send reservation & login info");
            }

            String pw = (guestPassword == null || guestPassword.trim().isEmpty()) ? "123456" : guestPassword.trim();

            guestId = guestService.ensureGuestWithPassword(
                    guestName, guestEmail, guestContactNumber, pw
            );

            passwordForEmail = pw;
        }

        boolean overlap = dao.hasOverlappingReservation(roomId, checkIn, checkOut);
        if (overlap) throw new IllegalArgumentException("Selected dates overlap with an existing reservation.");

        int nights = calcNights(checkIn, checkOut);

        double rate = roomDao.findPriceById(roomId);
        if (rate <= 0) throw new IllegalArgumentException("Room price not found");

        double subtotal = round2(nights * rate);
        double tax = round2(subtotal * (taxRateVal / 100.0));
        double total = round2(subtotal + tax - discountVal);
        if (total < 0) total = 0;

        String number = generateReservationNumber(checkIn);

        Reservation r = new Reservation();
        r.setReservationNumber(number);
        r.setGuestId(guestId);
        r.setRoomId(roomId);
        r.setCheckInDate(checkIn);
        r.setCheckOutDate(checkOut);
        r.setStatus(status);
        r.setNotes(notes);
        r.setCreatedBy(createdBy);

        r.setNights(nights);
        r.setRatePerNight(rate);
        r.setSubtotal(subtotal);
        r.setTax(tax);
        r.setDiscount(discountVal);
        r.setTotalAmount(total);

        int id = dao.create(r);
        if (id <= 0) throw new IllegalStateException("Reservation insert failed");

        try {
            sendReservationEmail(guestEmail, guestName, passwordForEmail, r, id);
        } catch (Exception ignored) {
           
        }

        return id;
    }

    private void sendReservationEmail(String toEmail, String guestName, String guestPassword, Reservation r, int reservationId) {
        if (toEmail == null || toEmail.trim().isEmpty()) return;

        String subject = "OceanView Reservation Confirmation - " + r.getReservationNumber();

        StringBuilder body = new StringBuilder();
        body.append("Hello ").append(safe(guestName)).append(",\n\n");
        body.append("Your reservation has been created successfully.\n\n");
        body.append("Reservation ID: ").append(reservationId).append("\n");
        body.append("Reservation Number: ").append(r.getReservationNumber()).append("\n");
        body.append("Check-in: ").append(r.getCheckInDate()).append("\n");
        body.append("Check-out: ").append(r.getCheckOutDate()).append("\n");
        body.append("Nights: ").append(r.getNights()).append("\n");
        body.append("Rate per night: ").append(r.getRatePerNight()).append("\n");
        body.append("Subtotal: ").append(r.getSubtotal()).append("\n");
        body.append("Tax: ").append(r.getTax()).append("\n");
        body.append("Discount: ").append(r.getDiscount()).append("\n");
        body.append("Total: ").append(r.getTotalAmount()).append("\n");
        body.append("Status: ").append(r.getStatus()).append("\n");

        if (r.getNotes() != null && !r.getNotes().trim().isEmpty()) {
            body.append("Notes: ").append(r.getNotes().trim()).append("\n");
        }

      
        if (guestPassword != null && !guestPassword.trim().isEmpty()) {
            body.append("\nGuest Login Details:\n");
            body.append("Email: ").append(toEmail.trim()).append("\n");
            body.append("Password: ").append(guestPassword.trim()).append("\n");
        }

        body.append("\nThank you,\nOceanView Resort");

        EmailUtil.send(toEmail.trim(), subject, body.toString());
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) status = "PENDING";
        status = status.trim().toUpperCase();

        if (!status.equals("PENDING") && !status.equals("CONFIRMED") && !status.equals("CHECKED_IN")) {
            throw new IllegalArgumentException("Invalid status");
        }

        return status;
    }

    private int calcNights(Date checkIn, Date checkOut) {
        long ms = checkOut.getTime() - checkIn.getTime();
        int nights = (int) (ms / (1000L * 60 * 60 * 24));
        if (nights <= 0) throw new IllegalArgumentException("Invalid date range");
        return nights;
    }

    private String generateReservationNumber(Date checkIn) {
        String yyyymmdd = new SimpleDateFormat("yyyyMMdd").format(checkIn);
        String prefix = "RES-" + yyyymmdd + "-";

        String last = dao.findLastReservationNumberForDate(prefix);
        int next = 1;

        if (last != null && last.startsWith(prefix)) {
            String tail = last.substring(prefix.length());
            try {
                next = Integer.parseInt(tail) + 1;
            } catch (Exception ignored) {
                next = 1;
            }
        }

        return prefix + String.format("%03d", next);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
