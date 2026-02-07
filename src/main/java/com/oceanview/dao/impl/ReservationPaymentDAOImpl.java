// ReservationPaymentDAOImpl.java
package com.oceanview.dao.impl;

import com.oceanview.dao.ReservationPaymentDAO;
import com.oceanview.model.ReservationPayment;
import com.oceanview.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationPaymentDAOImpl implements ReservationPaymentDAO {

    @Override
    public int create(ReservationPayment payment) {

        String sql =
                "INSERT INTO reservation_payments " +
                        "(reservation_id, paid_amount, paid_date, method, note, created_by) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, payment.getReservationId());
            ps.setDouble(2, payment.getPaidAmount());
            ps.setTimestamp(3, payment.getPaidDate());
            ps.setString(4, payment.getMethod());
            ps.setString(5, payment.getNote());
            ps.setInt(6, payment.getCreatedBy());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                return 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create payment", e);
        }
    }

    @Override
    public List<ReservationPayment> findByReservation(int reservationId) {

        String sql =
                "SELECT payment_id, reservation_id, paid_amount, paid_date, method, note, created_by " +
                        "FROM reservation_payments " +
                        "WHERE reservation_id = ? " +
                        "ORDER BY paid_date DESC";

        List<ReservationPayment> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, reservationId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }

            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load payment history", e);
        }
    }

    @Override
    public double sumPaymentsByReservation(int reservationId) {

        String sql =
                "SELECT COALESCE(SUM(paid_amount), 0) " +
                        "FROM reservation_payments " +
                        "WHERE reservation_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, reservationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to sum payments", e);
        }

        return 0.0;
    }

    @Override
    public boolean delete(int paymentId) {

        String sql = "DELETE FROM reservation_payments WHERE payment_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, paymentId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete payment", e);
        }
    }

    @Override
    public ReservationPayment findById(int paymentId) {

        String sql =
                "SELECT payment_id, reservation_id, paid_amount, paid_date, method, note, created_by " +
                        "FROM reservation_payments " +
                        "WHERE payment_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, paymentId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payment by id", e);
        }
    }

    private ReservationPayment map(ResultSet rs) throws SQLException {

        ReservationPayment p = new ReservationPayment();

        p.setPaymentId(rs.getInt("payment_id"));
        p.setReservationId(rs.getInt("reservation_id"));
        p.setPaidAmount(rs.getDouble("paid_amount"));
        p.setPaidDate(rs.getTimestamp("paid_date"));
        p.setMethod(rs.getString("method"));
        p.setNote(rs.getString("note"));
        p.setCreatedBy(rs.getInt("created_by"));

        return p;
    }
}
