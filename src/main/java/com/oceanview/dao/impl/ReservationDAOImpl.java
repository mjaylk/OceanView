package com.oceanview.dao.impl;

import com.oceanview.dao.ReservationDAO;
import com.oceanview.model.ReservationDailyCount;


import com.oceanview.model.Reservation;
import com.oceanview.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAOImpl implements ReservationDAO {

    @Override
    public List<Reservation> findAll() {
        String sql =
            "SELECT r.reservation_id, r.reservation_number, r.guest_id, r.room_id, " +
            "       r.check_in_date, r.check_out_date, r.status, r.notes, r.created_by, " +
            "       r.nights, r.rate_per_night, r.subtotal, r.tax, r.discount, r.total_amount, " +
            "       g.full_name AS guest_name, g.email AS guest_email, g.contact_number AS guest_phone, " +
            "       rm.room_number AS room_number, rm.room_type AS room_type " +
            "FROM reservations r " +
            "LEFT JOIN guests g ON g.guest_id = r.guest_id " +
            "LEFT JOIN rooms rm ON rm.room_id = r.room_id " +
            "ORDER BY r.reservation_id DESC";

        List<Reservation> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(map(rs));
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load reservations", e);
        }
    }

    @Override
    public Reservation findById(int id) {
        String sql =
            "SELECT r.reservation_id, r.reservation_number, r.guest_id, r.room_id, " +
            "       r.check_in_date, r.check_out_date, r.status, r.notes, r.created_by, " +
            "       r.nights, r.rate_per_night, r.subtotal, r.tax, r.discount, r.total_amount, " +
            "       g.full_name AS guest_name, g.email AS guest_email, g.contact_number AS guest_phone, " +
            "       rm.room_number AS room_number, rm.room_type AS room_type " +
            "FROM reservations r " +
            "LEFT JOIN guests g ON g.guest_id = r.guest_id " +
            "LEFT JOIN rooms rm ON rm.room_id = r.room_id " +
            "WHERE r.reservation_id=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation by id", e);
        }
    }

    @Override
    public Reservation findByNumber(String reservationNumber) {
        String sql =
            "SELECT r.reservation_id, r.reservation_number, r.guest_id, r.room_id, " +
            "       r.check_in_date, r.check_out_date, r.status, r.notes, r.created_by, " +
            "       r.nights, r.rate_per_night, r.subtotal, r.tax, r.discount, r.total_amount, " +
            "       g.full_name AS guest_name, g.email AS guest_email, g.contact_number AS guest_phone, " +
            "       rm.room_number AS room_number, rm.room_type AS room_type " +
            "FROM reservations r " +
            "LEFT JOIN guests g ON g.guest_id = r.guest_id " +
            "LEFT JOIN rooms rm ON rm.room_id = r.room_id " +
            "WHERE r.reservation_number=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, reservationNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation by number", e);
        }
    }

    @Override
    public boolean hasOverlappingReservation(int roomId, Date checkIn, Date checkOut) {
        String sql =
            "SELECT COUNT(*) " +
            "FROM reservations " +
            "WHERE room_id=? " +
            "  AND status IN ('CONFIRMED','CHECKED_IN','PENDING') " +
            "  AND check_in_date < ? " +
            "  AND check_out_date > ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);
            ps.setDate(2, checkOut);
            ps.setDate(3, checkIn);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check overlap", e);
        }
    }

    @Override
    public boolean hasOverlappingReservationExceptSelf(int roomId, int reservationId, Date checkIn, Date checkOut) {
        String sql =
            "SELECT COUNT(*) " +
            "FROM reservations " +
            "WHERE room_id=? " +
            "  AND reservation_id <> ? " +
            "  AND status IN ('CONFIRMED','CHECKED_IN','PENDING') " +
            "  AND check_in_date < ? " +
            "  AND check_out_date > ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);
            ps.setInt(2, reservationId);
            ps.setDate(3, checkOut);
            ps.setDate(4, checkIn);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check overlap (except self)", e);
        }
    }

    @Override
    public int create(Reservation r) {
        String sql =
            "INSERT INTO reservations " +
            "(reservation_number, guest_id, room_id, check_in_date, check_out_date, " +
            " nights, rate_per_night, subtotal, tax, discount, total_amount, status, notes, created_by) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, r.getReservationNumber());
            ps.setInt(2, r.getGuestId());
            ps.setInt(3, r.getRoomId());
            ps.setDate(4, r.getCheckInDate());
            ps.setDate(5, r.getCheckOutDate());
            ps.setInt(6, r.getNights());
            ps.setDouble(7, r.getRatePerNight());
            ps.setDouble(8, r.getSubtotal());
            ps.setDouble(9, r.getTax());
            ps.setDouble(10, r.getDiscount());
            ps.setDouble(11, r.getTotalAmount());
            ps.setString(12, r.getStatus());
            ps.setString(13, r.getNotes() == null ? "" : r.getNotes());
            ps.setInt(14, r.getCreatedBy());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    updateRoomStatus(conn, r.getRoomId());
                    return id;
                }
                return 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create reservation", e);
        }
    }


    @Override
    public boolean updateStatus(int reservationId, String status) {
        String sql = "UPDATE reservations SET status=? WHERE reservation_id=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, reservationId);
            boolean updated = ps.executeUpdate() > 0;
            
            if (updated) {
                Reservation r = findById(reservationId);
                if (r != null) {
                    updateRoomStatus(conn, r.getRoomId());
                }
            }
            
            return updated;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation status", e);
        }
    }

    @Override
    public boolean update(Reservation r) {
        String sql =
            "UPDATE reservations SET guest_id=?, room_id=?, check_in_date=?, check_out_date=?, status=?, notes=?, " +
            "nights=?, rate_per_night=?, subtotal=?, tax=?, discount=?, total_amount=? " +
            "WHERE reservation_id=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int oldRoomId = 0;
            Reservation old = findById(r.getReservationId());
            if (old != null) {
                oldRoomId = old.getRoomId();
            }

            ps.setInt(1, r.getGuestId());
            ps.setInt(2, r.getRoomId());
            ps.setDate(3, r.getCheckInDate());
            ps.setDate(4, r.getCheckOutDate());
            ps.setString(5, r.getStatus());
            ps.setString(6, r.getNotes());

            ps.setInt(7, r.getNights());
            ps.setDouble(8, r.getRatePerNight());
            ps.setDouble(9, r.getSubtotal());
            ps.setDouble(10, r.getTax());
            ps.setDouble(11, r.getDiscount());
            ps.setDouble(12, r.getTotalAmount());

            ps.setInt(13, r.getReservationId());

            boolean updated = ps.executeUpdate() > 0;
            
            if (updated) {
                updateRoomStatus(conn, r.getRoomId());
                if (oldRoomId > 0 && oldRoomId != r.getRoomId()) {
                    updateRoomStatus(conn, oldRoomId);
                }
            }
            
            return updated;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    @Override
    public boolean delete(int reservationId) {
        String sql = "DELETE FROM reservations WHERE reservation_id=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            Reservation r = findById(reservationId);
            int roomId = (r != null) ? r.getRoomId() : 0;

            ps.setInt(1, reservationId);
            boolean deleted = ps.executeUpdate() > 0;
            
            if (deleted && roomId > 0) {
                updateRoomStatus(conn, roomId);
            }
            
            return deleted;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete reservation", e);
        }
    }
    
    @Override
    public boolean hasBookingInRange(int roomId, Date checkIn, Date checkOut) {
        String sql =
                "SELECT COUNT(*) " +
                "FROM reservations " +
                "WHERE room_id=? " +
                "AND UPPER(status) IN ('PENDING','CONFIRMED','CHECKED_IN') " +
                "AND check_in_date < ? " +
                "AND check_out_date > ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);
            ps.setDate(2, checkOut);
            ps.setDate(3, checkIn);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check booking in range", e);
        }
    }

    private void updateRoomStatus(Connection conn, int roomId) throws SQLException {
        String checkSql = 
            "SELECT COUNT(*) FROM reservations " +
            "WHERE room_id = ? " +
            "AND UPPER(status) IN ('PENDING','CONFIRMED','CHECKED_IN') " +
            "AND check_in_date <= CURDATE() " +
            "AND check_out_date > CURDATE()";
        
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                boolean hasActiveBooking = rs.getInt(1) > 0;
                
                String currentStatusSql = "SELECT status FROM rooms WHERE room_id = ?";
                String currentStatus = "AVAILABLE";
                try (PreparedStatement ps2 = conn.prepareStatement(currentStatusSql)) {
                    ps2.setInt(1, roomId);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        if (rs2.next()) {
                            currentStatus = rs2.getString(1);
                        }
                    }
                }
                
                if ("MAINTENANCE".equalsIgnoreCase(currentStatus)) {
                    return;
                }
                
                String newStatus = hasActiveBooking ? "BOOKED" : "AVAILABLE";
                String updateSql = "UPDATE rooms SET status = ? WHERE room_id = ?";
                try (PreparedStatement ps3 = conn.prepareStatement(updateSql)) {
                    ps3.setString(1, newStatus);
                    ps3.setInt(2, roomId);
                    ps3.executeUpdate();
                }
            }
        }
    }

    @Override
    public String findLastReservationNumberForDate(String yyyymmddPrefix) {
        String sql =
            "SELECT reservation_number " +
            "FROM reservations " +
            "WHERE reservation_number LIKE ? " +
            "ORDER BY reservation_number DESC " +
            "LIMIT 1";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, yyyymmddPrefix + "%");

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get last reservation number", e);
        }
    }

    @Override
    public List<Reservation> findByRoom(int roomId) {
        String sql =
            "SELECT reservation_id, reservation_number, guest_id, room_id, check_in_date, check_out_date, status, notes, created_by, " +
            "       nights, rate_per_night, subtotal, tax, discount, total_amount " +
            "FROM reservations " +
            "WHERE room_id=? AND status IN ('CONFIRMED','CHECKED_IN') " +
            "ORDER BY check_in_date ASC";

        List<Reservation> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapBasic(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load reservations by room", e);
        }
    }

    @Override
    public List<Reservation> findBetween(Date start, Date end) {
        String sql =
            "SELECT r.reservation_id, r.reservation_number, r.guest_id, r.room_id, r.check_in_date, r.check_out_date, " +
            "       r.status, r.notes, r.created_by, r.nights, r.rate_per_night, r.subtotal, r.tax, r.discount, r.total_amount, " +
            "       g.full_name AS guest_name, g.email AS guest_email, g.contact_number AS guest_phone, " +
            "       rm.room_number AS room_number, rm.room_type AS room_type " +
            "FROM reservations r " +
            "LEFT JOIN guests g ON g.guest_id = r.guest_id " +
            "LEFT JOIN rooms rm ON rm.room_id = r.room_id " +
            "WHERE r.check_in_date < ? AND r.check_out_date > ? " +
            "  AND r.status IN ('CONFIRMED','CHECKED_IN','PENDING') " +
            "ORDER BY r.check_in_date ASC";

        List<Reservation> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, end);
            ps.setDate(2, start);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load reservations between dates", e);
        }
    }

    private Reservation mapBasic(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setReservationId(rs.getInt("reservation_id"));
        r.setReservationNumber(rs.getString("reservation_number"));
        r.setGuestId(rs.getInt("guest_id"));
        r.setRoomId(rs.getInt("room_id"));
        r.setCheckInDate(rs.getDate("check_in_date"));
        r.setCheckOutDate(rs.getDate("check_out_date"));
        r.setStatus(rs.getString("status"));
        r.setNotes(rs.getString("notes"));
        r.setCreatedBy(rs.getInt("created_by"));

        r.setNights(rs.getInt("nights"));
        r.setRatePerNight(rs.getDouble("rate_per_night"));
        r.setSubtotal(rs.getDouble("subtotal"));
        r.setTax(rs.getDouble("tax"));
        r.setDiscount(rs.getDouble("discount"));
        r.setTotalAmount(rs.getDouble("total_amount"));
        return r;
    }

    
    
    private Reservation map(ResultSet rs) throws SQLException {
        Reservation r = mapBasic(rs);

        r.setGuestName(rs.getString("guest_name"));
        r.setGuestEmail(rs.getString("guest_email"));
        r.setGuestContactNumber(rs.getString("guest_phone"));
        r.setRoomNumber(rs.getString("room_number"));
        r.setRoomType(rs.getString("room_type"));

        return r;
    }
    
    @Override
    public List<Reservation> getRecentCheckins() {
        String sql =
            "SELECT r.reservation_id, r.reservation_number, r.guest_id, r.room_id, " +
            "       r.check_in_date, r.check_out_date, r.status, r.notes, r.created_by, " +
            "       r.nights, r.rate_per_night, r.subtotal, r.tax, r.discount, r.total_amount, " +
            "       COALESCE(g.full_name, '') AS guest_name, " +
            "       COALESCE(g.email, '') AS guest_email, " +
            "       COALESCE(g.contact_number, '') AS guest_phone, " +  
            "       COALESCE(rm.room_number, 'N/A') AS room_number, " +
            "       COALESCE(rm.room_type, 'Unknown') AS room_type " +
            "FROM reservations r " +
            "LEFT JOIN guests g ON g.guest_id = r.guest_id " +
            "LEFT JOIN rooms rm ON rm.room_id = r.room_id " +
            "WHERE r.check_in_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
            "  AND r.status IN ('CHECKED_IN', 'CONFIRMED') " +
            "ORDER BY r.check_in_date DESC, r.reservation_id DESC " +
            "LIMIT 5";

        List<Reservation> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load recent check-ins", e);
        }
    }
    
    @Override
    public List<Reservation> findByGuestId(int guestId) {
        String sql =
                "SELECT r.reservation_id, r.reservation_number, r.guest_id, r.room_id, " +
                "r.check_in_date, r.check_out_date, r.status, r.notes, r.nights, " +
                "r.rate_per_night, r.subtotal, r.tax, r.discount, r.total_amount, " +
                "rm.room_number, rm.room_type " +
                "FROM reservations r " +
                "JOIN rooms rm ON r.room_id = rm.room_id " +
                "WHERE r.guest_id = ? " +
                "ORDER BY r.reservation_id DESC";

        List<Reservation> list = new java.util.ArrayList<>();

        try (java.sql.Connection con = com.oceanview.util.DatabaseConnection.getInstance().getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, guestId);

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reservation r = new Reservation();
                    r.setReservationId(rs.getInt("reservation_id"));
                    r.setReservationNumber(rs.getString("reservation_number"));
                    r.setGuestId(rs.getInt("guest_id"));
                    r.setRoomId(rs.getInt("room_id"));
                    r.setCheckInDate(rs.getDate("check_in_date"));
                    r.setCheckOutDate(rs.getDate("check_out_date"));
                    r.setStatus(rs.getString("status"));
                    r.setNotes(rs.getString("notes"));

                    r.setNights(rs.getInt("nights"));
                    r.setRatePerNight(rs.getDouble("rate_per_night"));
                    r.setSubtotal(rs.getDouble("subtotal"));
                    r.setTax(rs.getDouble("tax"));
                    r.setDiscount(rs.getDouble("discount"));
                    r.setTotalAmount(rs.getDouble("total_amount"));

                    r.setRoomNumber(rs.getString("room_number"));
                    r.setRoomType(rs.getString("room_type"));

                    list.add(r);
                }
            }

            return list;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load reservations for guest", e);
        }
    }

    @Override
    public int countBetween(Date start, Date end) {
        String sql =
                "SELECT COUNT(*) " +
                "FROM reservations " +
                "WHERE check_in_date >= ? AND check_in_date <= ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, start);
            ps.setDate(2, end);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to count reservations", e);
        }

        return 0;
    }

    @Override
    public double sumRevenueBetween(Date start, Date end) {
        String sql =
                "SELECT COALESCE(SUM(total_amount), 0) " +
                "FROM reservations " +
                "WHERE check_in_date >= ? AND check_in_date <= ? " +
                "AND UPPER(status) IN ('CONFIRMED','CHECKED_IN','COMPLETED')";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, start);
            ps.setDate(2, end);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to sum revenue", e);
        }

        return 0.0;
    }

    
    @Override
    public int countAllReservations() {
        String sql = "SELECT COUNT(*) FROM reservations";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to count all reservations", e);
        }
    }

    @Override
    public int countOccupiedRoomsToday(Date today) {
        String sql =
                "SELECT COUNT(DISTINCT room_id) " +
                "FROM reservations " +
                "WHERE UPPER(status) IN ('CONFIRMED','CHECKED_IN') " +
                "AND check_in_date <= ? " +
                "AND check_out_date > ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, today);
            ps.setDate(2, today);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to count occupied rooms today", e);
        }
    }


    
    @Override
    public List<ReservationDailyCount> countPerDayBetween(Date start, Date end) {
        String sql =
                "SELECT check_in_date AS day, COUNT(*) AS cnt " +
                "FROM reservations " +
                "WHERE check_in_date >= ? AND check_in_date <= ? " +
                "GROUP BY check_in_date " +
                "ORDER BY check_in_date";

        List<ReservationDailyCount> out = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, start);
            ps.setDate(2, end);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date day = rs.getDate("day");
                    int cnt = rs.getInt("cnt");
                    out.add(new ReservationDailyCount(day, cnt));
                }
            }

            return out;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to count reservations per day", e);
        }
    }
}