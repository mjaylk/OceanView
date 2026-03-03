package com.oceanview.service;

import com.oceanview.dao.ReservationDAO;
import com.oceanview.dao.ReservationPaymentDAO;
import com.oceanview.dao.RoomDAO;
import com.oceanview.dao.impl.ReservationDAOImpl;
import com.oceanview.dao.impl.ReservationPaymentDAOImpl;
import com.oceanview.dao.impl.RoomDAOImpl;
import com.oceanview.model.Reservation;
import com.oceanview.model.ReservationDailyCount;
import com.oceanview.model.ReservationPayment;
import com.oceanview.model.Room;
import com.oceanview.util.EmailUtil;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.security.SecureRandom;

public class ReservationService {

    private final ReservationDAO dao = new ReservationDAOImpl();
    private final GuestService guestService = new GuestService();
    private final RoomDAO roomDao = new RoomDAOImpl();
    private final ReservationPaymentDAO paymentDAO = new ReservationPaymentDAOImpl();

    private static final double DEFAULT_TAX_RATE = 0.0;

    // READ
  

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

    public List<Reservation> getRecentCheckins() {
        return dao.getRecentCheckins();
    }

    public List<Reservation> listReservationsByGuest(int guestId) {
        if (guestId <= 0) throw new IllegalArgumentException("Invalid guestId");
        return dao.findByGuestId(guestId);
    }

   
    // DELETE
   

    public boolean deleteReservation(int id) {
        if (id <= 0) throw new IllegalArgumentException("Invalid reservation id");

        Reservation existing = dao.findById(id);
        if (existing == null) return false;

        // delete associated payments first
        int paymentCount = paymentDAO.countByReservation(id);
        if (paymentCount > 0) {
            List<ReservationPayment> payments = paymentDAO.findByReservation(id);
            for (ReservationPayment payment : payments) {
                paymentDAO.delete(payment.getPaymentId());
            }
        }

        boolean ok = dao.delete(id);

        if (ok) {
            syncRoomStatusAfterChange(existing.getRoomId());
        }

        return ok;
    }

  
    // ROOMS WITH AVAILABILITY
  

    public String listRoomsWithAvailabilityJson(Date checkIn, Date checkOut) {
        if (checkIn == null || checkOut == null)
            throw new IllegalArgumentException("Check-in/out required");
        if (checkOut.compareTo(checkIn) <= 0)
            throw new IllegalArgumentException("Check-out must be after check-in");

        List<Room> rooms = dao.findAll() != null ? roomDao.findAll() : List.of();

        StringBuilder sb = new StringBuilder("{\"success\":true,\"rooms\":[");
        for (int i = 0; i < rooms.size(); i++) {
            Room r = rooms.get(i);

            String baseStatus = (r.getStatus() == null) ? "" : r.getStatus().trim().toUpperCase();
            boolean maintenance = "MAINTENANCE".equals(baseStatus);

            boolean booked = false;
            if (!maintenance) {
                booked = dao.hasBookingInRange(r.getRoomId(), checkIn, checkOut);
            }

            String computed = maintenance ? "MAINTENANCE" : (booked ? "BOOKED" : "AVAILABLE");

            sb.append("{")
                    .append("\"roomId\":").append(r.getRoomId()).append(",")
                    .append("\"roomNumber\":\"").append(esc(r.getRoomNumber())).append("\",")
                    .append("\"roomType\":\"").append(esc(r.getRoomType())).append("\",")
                    .append("\"price\":").append(r.getRatePerNight()).append(",")
                    .append("\"maxGuests\":").append(r.getMaxGuests()).append(",")
                    .append("\"status\":\"").append(computed).append("\"")
                    .append("}");

            if (i < rooms.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    // CREATE
  

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
        // validation
        if (roomId <= 0) throw new IllegalArgumentException("Room is required");
        if (checkIn == null || checkOut == null) throw new IllegalArgumentException("Check-in/out required");
        if (checkOut.compareTo(checkIn) <= 0) throw new IllegalArgumentException("Check-out must be after check-in");
        if (createdBy <= 0) throw new IllegalArgumentException("Session expired. Please login again.");

        status = normalizeStatus(status);

        double taxRateVal = (taxRate == null) ? DEFAULT_TAX_RATE : taxRate;
        double discountVal = (discount == null) ? 0.0 : discount;
        if (taxRateVal < 0) taxRateVal = 0;
        if (discountVal < 0) discountVal = 0;

        // email is required
        if (guestEmail == null || guestEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Guest email required to send reservation & login info");
        }

        String email = guestEmail.trim();

        // pick or generate password
        String pw = (guestPassword == null || guestPassword.trim().isEmpty())
                ? generateTempPassword(8)
                : guestPassword.trim();

        // GUEST RESOLUTION 
        if (guestId <= 0) {
      
            int existingId = guestService.getGuestIdByEmail(email);
            if (existingId > 0) {
               
                guestId = existingId;
                guestService.updateGuestPassword(guestId, pw);
            } else {
            
                guestId = guestService.ensureGuestWithPassword(guestName, email, guestContactNumber, pw);
                if (guestId <= 0) throw new IllegalStateException("Guest create failed");
            }
        } else {
         
            String existingEmail = guestService.getEmailByGuestId(guestId);
            if (existingEmail == null || !existingEmail.equalsIgnoreCase(email)) {
               
                int existingId = guestService.getGuestIdByEmail(email);
                if (existingId > 0) {
                   
                    guestId = existingId;
                    guestService.updateGuestPassword(guestId, pw);
                } else {
                   
                    guestId = guestService.ensureGuestWithPassword(guestName, email, guestContactNumber, pw);
                    if (guestId <= 0) throw new IllegalStateException("Guest create failed");
                }
            } else {
                // guestId matches the email — just update password
                guestService.updateGuestPassword(guestId, pw);
            }
        }
        // END GUEST RESOLUTION

        // overlap check
        boolean overlap = dao.hasOverlappingReservation(roomId, checkIn, checkOut);
        if (overlap) throw new IllegalArgumentException("Selected dates overlap with an existing reservation.");

        int nights = calcNights(checkIn, checkOut);

        double rate = roomDao.findPriceById(roomId);
        if (rate <= 0) throw new IllegalArgumentException("Room price not found");

        // pricing calc
        double subtotal = round2(nights * rate);
        double tax      = round2(subtotal * (taxRateVal / 100.0));
        double total    = round2(subtotal + tax - discountVal);
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

        if (isActiveBookingStatus(status)) {
            roomDao.updateStatus(roomId, "BOOKED");
        }

        // send confirmation email
        try {
            sendReservationEmail(email, guestName, pw, r, id);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return id;
    }

 
    // UPDATE


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
        // validation
        if (reservationId <= 0) throw new IllegalArgumentException("Invalid reservation");
        if (roomId <= 0) throw new IllegalArgumentException("Room is required");
        if (checkIn == null || checkOut == null) throw new IllegalArgumentException("Check-in/out required");
        if (checkOut.compareTo(checkIn) <= 0) throw new IllegalArgumentException("Check-out must be after check-in");
        if (updatedBy <= 0) throw new IllegalArgumentException("Session expired. Please login again.");

        status = normalizeStatus(status);

        if (taxRate < 0) taxRate = 0;
        if (discount < 0) discount = 0;

     
        if (guestId <= 0) {
            guestId = guestService.ensureGuest(guestName, guestEmail, guestContactNumber);
        } else {
           
        	guestService.updateGuest(guestId, guestName, null, guestContactNumber, guestEmail);

        }

        Reservation existing = dao.findById(reservationId);
        if (existing == null) throw new IllegalArgumentException("Reservation not found");

        int oldRoomId = existing.getRoomId();

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

        // pricing calc
        double subtotal = round2(nights * rate);
        double tax      = round2(subtotal * (taxRate / 100.0));
        double total    = round2(subtotal + tax - discount);
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

        boolean ok = dao.update(r);

        if (ok) {
            if (oldRoomId != roomId) {
                // room changed — update new room and sync old one
                if (isActiveBookingStatus(status)) roomDao.updateStatus(roomId, "BOOKED");
                syncRoomStatusAfterChange(oldRoomId);
            } else {
                // same room — update or release based on status
                if (isActiveBookingStatus(status)) {
                    roomDao.updateStatus(roomId, "BOOKED");
                } else {
                    syncRoomStatusAfterChange(roomId);
                }
            }
        }

        return ok;
    }

  
    // DASHBOARD STATS
  
public String getDashboardStatsJson(int days) {
    if (days <= 0) days = 30;

    LocalDate today   = LocalDate.now();
    LocalDate startLd = today.minusDays(days - 1);
    Date start        = Date.valueOf(startLd);
    Date end          = Date.valueOf(today);

    int totalReservations   = dao.countBetween(start, end);
    LocalDate monthStartLd  = today.withDayOfMonth(1);
    Date monthStart         = Date.valueOf(monthStartLd);
    double revenueThisMonth = dao.sumRevenueBetween(monthStart, end);

    //  unique guests & occupancy 
    List<Reservation> allInRange = dao.findBetween(start, end);

    java.util.Set<Integer> uniqueGuestIds = new java.util.HashSet<>();
    int activeCount = 0;

    for (Reservation r : allInRange) {
        if (r.getGuestId() > 0) uniqueGuestIds.add(r.getGuestId());

        String s = r.getStatus() == null ? "" : r.getStatus().trim().toUpperCase();
        if (s.equals("CONFIRMED") || s.equals("CHECKED_IN") || s.equals("BOOKED")) {
            activeCount++;
        }
    }

    int uniqueGuests = uniqueGuestIds.size();
    double occupancyRate = allInRange.isEmpty()
            ? 0.0
            : round2((activeCount * 100.0) / allInRange.size());
   

    // build empty daily series
    LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
    for (int i = 0; i < days; i++) {
        map.put(startLd.plusDays(i).toString(), 0);
    }

    // fill from DB
    List<ReservationDailyCount> rows = dao.countPerDayBetween(start, end);
    for (ReservationDailyCount row : rows) {
        if (row == null || row.getDay() == null) continue;
        String key = row.getDay().toString();
        if (map.containsKey(key)) map.put(key, row.getCount());
    }

    // build JSON
    StringBuilder sb = new StringBuilder();
    sb.append("{\"success\":true,");
    sb.append("\"totalReservations\":").append(totalReservations).append(",");
    sb.append("\"revenueThisMonth\":").append(String.format(Locale.US, "%.2f", revenueThisMonth)).append(",");
    sb.append("\"uniqueGuests\":").append(uniqueGuests).append(",");                          // NEW
    sb.append("\"occupancyRate\":\"").append(String.format(Locale.US, "%.1f", occupancyRate)).append("\","); // NEW
    sb.append("\"series\":[");

    int i = 0;
    for (Map.Entry<String, Integer> e : map.entrySet()) {
        sb.append("{\"label\":\"").append(e.getKey()).append("\",\"count\":").append(e.getValue()).append("}");
        if (i < map.size() - 1) sb.append(",");
        i++;
    }

    sb.append("]}");
    return sb.toString();
}

 
    // PRIVATE HELPERS

    private boolean isActiveBookingStatus(String status) {
        if (status == null) return false;
        String s = status.trim().toUpperCase();
        return s.equals("CONFIRMED") || s.equals("BOOKED") || s.equals("CHECKED_IN");
    }

    private void syncRoomStatusAfterChange(int roomId) {
        if (roomId <= 0) return;

        Room room = roomDao.findById(roomId);
        if (room == null) return;

        String baseStatus = (room.getStatus() == null) ? "" : room.getStatus().trim().toUpperCase();
        if ("MAINTENANCE".equals(baseStatus)) return;

        Date today = Date.valueOf(LocalDate.now());
        boolean hasActiveToday = dao.hasBookingInRange(
                roomId, today, Date.valueOf(today.toLocalDate().plusDays(1))
        );

        roomDao.updateStatus(roomId, hasActiveToday ? "BOOKED" : "AVAILABLE");
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) status = "PENDING";
        status = status.trim().toUpperCase();

        if (!status.equals("PENDING")
                && !status.equals("CONFIRMED")
                && !status.equals("CANCELLED")
                && !status.equals("CHECKED_OUT")
                && !status.equals("CHECKED_IN")) {
            throw new IllegalArgumentException("Invalid status: " + status);
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
        String prefix   = "RES-" + yyyymmdd + "-";

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

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String nvl(Object v) {
        return v == null ? "-" : String.valueOf(v);
    }

    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ" +
            "abcdefghijkmnopqrstuvwxyz" +
            "23456789" +
            "@#$%";

    private static final SecureRandom RANDOM = new SecureRandom();

    private String generateTempPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    private void sendReservationEmail(String toEmail, String guestName, String guestPassword, Reservation r, int reservationId) {
        if (toEmail == null || toEmail.trim().isEmpty()) return;
        if (r == null) return;

        String email = toEmail.trim();
        String name  = (guestName == null || guestName.trim().isEmpty()) ? "Guest" : guestName.trim();

        String subject  = "OceanView Reservation Confirmation - " + nvl(r.getReservationNumber());
        String baseUrl  = "http://localhost:8080/OceanViewResortBooking";
        String loginUrl = baseUrl + "/guest-login.html";

        StringBuilder body = new StringBuilder();
        body.append("Hello ").append(name).append(",\n\n");
        body.append("Your reservation has been created successfully.\n\n");
        body.append("Reservation ID: ").append(reservationId).append("\n");
        body.append("Reservation Number: ").append(nvl(r.getReservationNumber())).append("\n");
        body.append("Check-in: ").append(nvl(r.getCheckInDate())).append("\n");
        body.append("Check-out: ").append(nvl(r.getCheckOutDate())).append("\n");
        body.append("Nights: ").append(r.getNights()).append("\n");
        body.append("Rate per night: $").append(String.format("%.2f", r.getRatePerNight())).append("\n");
        body.append("Subtotal: $").append(String.format("%.2f", r.getSubtotal())).append("\n");
        body.append("Tax: $").append(String.format("%.2f", r.getTax())).append("\n");
        body.append("Discount: $").append(String.format("%.2f", r.getDiscount())).append("\n");
        body.append("Total: $").append(String.format("%.2f", r.getTotalAmount())).append("\n");
        body.append("Status: ").append(nvl(r.getStatus())).append("\n");

        if (r.getNotes() != null && !r.getNotes().trim().isEmpty()) {
            body.append("Notes: ").append(r.getNotes().trim()).append("\n");
        }

        if (guestPassword != null && !guestPassword.trim().isEmpty()) {
            body.append("\nGuest Login Details:\n");
            body.append("Login URL: ").append(loginUrl).append("\n");
            body.append("Email: ").append(email).append("\n");
            body.append("Temporary Password: ").append(guestPassword.trim()).append("\n");
        }

        body.append("\nThank you,\nOceanView Resort");

        EmailUtil.send(email, subject, body.toString());
    }
}
