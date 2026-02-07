package com.oceanview.dao.impl;

import com.oceanview.dao.UserDAO;
import com.oceanview.model.User;
import com.oceanview.util.DatabaseConnection;

import java.util.ArrayList;
import java.util.List;
import java.sql.*;

public class UserDAOImpl implements UserDAO {

    // dao implementation
  

    @Override
    public User findByUsername(String username) {

        // login lookup
        String sql = "SELECT user_id, username, password_hash, role, status FROM users WHERE username=?";

        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                // result mapping
                User u = new User();
                u.setUserId(rs.getInt("user_id"));
                u.setUsername(rs.getString("username"));
                u.setPasswordHash(rs.getString("password_hash"));
                u.setRole(rs.getString("role"));
                u.setStatus(rs.getString("status"));
                return u;
            }

        } catch (SQLException e) {
            throw new RuntimeException("DB error in findByUsername", e);
        }
    }

    @Override
    public User findById(int id) {

        // search by id
        String sql = "SELECT user_id, username, password_hash, role, status FROM users WHERE user_id=?";

        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<User> findAll() {

        // read all
        String sql = "SELECT user_id, username, password_hash, role, status FROM users ORDER BY user_id DESC";
        List<User> list = new ArrayList<>();

        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // loop mapping
            while (rs.next()) list.add(map(rs));
            return list;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int create(User user) {

        // insert user
        String sql = "INSERT INTO users (username, password_hash, role, status) VALUES (?,?,?,?)";

        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole());
            ps.setString(4, user.getStatus());

            ps.executeUpdate();

            // generated id
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean update(User user) {

        // update details
        String sql = "UPDATE users SET username=?, role=?, status=? WHERE user_id=?";

        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getRole());
            ps.setString(3, user.getStatus());
            ps.setInt(4, user.getUserId());

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean updatePassword(int id, String passwordHash) {

        // password update
        String sql = "UPDATE users SET password_hash=? WHERE user_id=?";

        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, passwordHash);
            ps.setInt(2, id);

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean deactivate(int id) {

        // soft delete
        String sql = "UPDATE users SET status='INACTIVE' WHERE user_id=?";

        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean delete(int id) {

        // hard delete
        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();

            // console log
            System.out.println("Deleted user_id=" + id + ", rows affected: " + rows);
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("Delete failed: " + e.getMessage());
            return false;
        }
    }

    private User map(ResultSet rs) throws SQLException {

        // result mapping
        User u = new User();

        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setStatus(rs.getString("status"));

        return u;
    }
}
