package com.oceanview.dao.impl;

import com.oceanview.dao.GuestDAO;
import com.oceanview.model.Guest;
import com.oceanview.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GuestDAOImpl implements GuestDAO {

    @Override
    public List<Guest> findAll() {
        String sql = "SELECT guest_id, user_id, full_name, address, contact_number, email, password " +
                "FROM guests ORDER BY guest_id DESC";

        List<Guest> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(map(rs));
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load guests", e);
        }
    }

    @Override
    public int getNextGuestId() {
        String sql = "SELECT COALESCE(MAX(guest_id), 0) + 1 AS next_id FROM guests";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            rs.next();
            return rs.getInt("next_id");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to generate next guest id: " + e.getMessage(), e);
        }
    }

    @Override
    public Guest findById(int id) {
        String sql = "SELECT guest_id, user_id, full_name, address, contact_number, email, password " +
                "FROM guests WHERE guest_id=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find guest by id", e);
        }
    }

    @Override
    public Guest findByEmail(String email) {
        String sql = "SELECT guest_id, user_id, full_name, address, contact_number, email, password " +
                "FROM guests WHERE email=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find guest by email", e);
        }
    }

    @Override
    public Guest findByContactNumber(String contactNumber) {
        String sql = "SELECT guest_id, user_id, full_name, address, contact_number, email, password " +
                "FROM guests WHERE contact_number=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, contactNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find guest by contact number", e);
        }
    }

    @Override
    public Guest findByEmailAndPassword(String email, String password) {
        String sql = "SELECT guest_id, user_id, full_name, address, contact_number, email, password " +
                "FROM guests WHERE email=? AND password=? LIMIT 1";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to login guest", e);
        }
    }

    @Override
    public List<Guest> search(String q, int limit) {
        String sql =
                "SELECT guest_id, user_id, full_name, address, contact_number, email, password " +
                        "FROM guests " +
                        "WHERE full_name LIKE ? OR email LIKE ? OR contact_number LIKE ? " +
                        "ORDER BY guest_id DESC " +
                        "LIMIT ?";

        List<Guest> list = new ArrayList<>();
        String like = "%" + q + "%";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setInt(4, Math.max(1, Math.min(limit, 20)));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }

            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to search guests", e);
        }
    }

    @Override
    public int create(Guest g) {
        String sql = "INSERT INTO guests (guest_id, user_id, full_name, address, contact_number, email, password) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int id = g.getGuestId() > 0 ? g.getGuestId() : getNextGuestId();
            ps.setInt(1, id);

            if (g.getUserId() == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, g.getUserId());

            ps.setString(3, g.getFullName());

            if (g.getAddress() == null || g.getAddress().trim().isEmpty()) ps.setNull(4, Types.VARCHAR);
            else ps.setString(4, g.getAddress().trim());

            ps.setString(5, g.getContactNumber());

            if (g.getEmail() == null || g.getEmail().trim().isEmpty()) ps.setNull(6, Types.VARCHAR);
            else ps.setString(6, g.getEmail().trim());

            if (g.getPassword() == null || g.getPassword().trim().isEmpty()) ps.setNull(7, Types.VARCHAR);
            else ps.setString(7, g.getPassword().trim());

            ps.executeUpdate();
            return id;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create guest: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Guest g) {
        String sql = "UPDATE guests SET user_id=?, full_name=?, address=?, contact_number=?, email=? " +
                "WHERE guest_id=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (g.getUserId() == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, g.getUserId());

            ps.setString(2, g.getFullName());
            ps.setString(3, g.getAddress());
            ps.setString(4, g.getContactNumber());
            ps.setString(5, g.getEmail());
            ps.setInt(6, g.getGuestId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update guest", e);
        }
    }

    @Override
    public boolean updatePasswordById(int guestId, String password) {
        String sql = "UPDATE guests SET password=? WHERE guest_id=?";
        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, password);
            ps.setInt(2, guestId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update guest password", e);
        }
    }
    
    @Override
    public boolean updatePassword(int guestId, String password) {
        String sql = "UPDATE guests SET password = ? WHERE guest_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, password);
            ps.setInt(2, guestId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update guest password", e);
        }
    }


    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM guests WHERE guest_id=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete guest", e);
        }
    }

    private Guest map(ResultSet rs) throws SQLException {
        Guest g = new Guest();
        g.setGuestId(rs.getInt("guest_id"));

        int uid = rs.getInt("user_id");
        g.setUserId(rs.wasNull() ? null : uid);

        g.setFullName(rs.getString("full_name"));
        g.setAddress(rs.getString("address"));
        g.setContactNumber(rs.getString("contact_number"));
        g.setEmail(rs.getString("email"));
        g.setPassword(rs.getString("password")); 
        return g;
    }
}
