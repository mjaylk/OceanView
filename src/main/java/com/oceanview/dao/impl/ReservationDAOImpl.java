package com.oceanview.dao.impl;

import com.oceanview.dao.ReservationDAO;
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
            "       r.check_in_date, r.check_out_date, r.status, r.created_by, " +
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
            "       r.check_in_date, r.check_out_date, r.status, r.created_by, " +
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
            "       r.check_in_date, r.check_out_date, r.status, r.created_by, " +
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
            "  AND status IN ('CONFIRMED','CHECKED_IN') " +
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
    public int create(Reservation r) {
        String sql =
            "INSERT INTO reservations " +
            "(reservation_number, guest_id, room_id, check_in_date, check_out_date, " +
            " nights, rate_per_night, subtotal, tax, discount, total_amount, status, created_by) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
            ps.setInt(13, r.getCreatedBy());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
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
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation status", e);
        }
    }

    @Override
    public boolean update(Reservation r) {
        String sql =
            "UPDATE reservations SET guest_id=?, room_id=?, check_in_date=?, check_out_date=?, status=?, " +
            "nights=?, rate_per_night=?, subtotal=?, tax=?, discount=?, total_amount=? " +
            "WHERE reservation_id=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, r.getGuestId());
            ps.setInt(2, r.getRoomId());
            ps.setDate(3, r.getCheckInDate());
            ps.setDate(4, r.getCheckOutDate());
            ps.setString(5, r.getStatus());

            ps.setInt(6, r.getNights());
            ps.setDouble(7, r.getRatePerNight());
            ps.setDouble(8, r.getSubtotal());
            ps.setDouble(9, r.getTax());
            ps.setDouble(10, r.getDiscount());
            ps.setDouble(11, r.getTotalAmount());

            ps.setInt(12, r.getReservationId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    @Override
    public boolean delete(int reservationId) {
        String sql = "DELETE FROM reservations WHERE reservation_id=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, reservationId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete reservation", e);
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
            "SELECT reservation_id, reservation_number, guest_id, room_id, check_in_date, check_out_date, status, created_by, " +
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

    // IMPORTANT: changed to JOIN so calendar can show guest/room on hover
    @Override
    public List<Reservation> findBetween(Date start, Date end) {
        String sql =
            "SELECT r.reservation_id, r.reservation_number, r.guest_id, r.room_id, r.check_in_date, r.check_out_date, " +
            "       r.status, r.created_by, r.nights, r.rate_per_night, r.subtotal, r.tax, r.discount, r.total_amount, " +
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
                while (rs.next()) list.add(map(rs)); // map(), not mapBasic()
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

        // Read by column alias (recommended vs relying on column order). [web:123]
        r.setGuestName(rs.getString("guest_name"));
        r.setGuestEmail(rs.getString("guest_email"));
        r.setGuestContactNumber(rs.getString("guest_phone"));
        r.setRoomNumber(rs.getString("room_number"));
        r.setRoomType(rs.getString("room_type"));

        return r;
    }
}
