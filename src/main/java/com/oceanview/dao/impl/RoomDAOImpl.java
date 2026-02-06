package com.oceanview.dao.impl;

import com.oceanview.dao.RoomDAO;
import com.oceanview.model.Room;
import com.oceanview.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomDAOImpl implements RoomDAO {

    @Override
    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT room_id, room_number, room_type, rate_per_night, max_guests, status, description, image_url " +
                     "FROM rooms ORDER BY room_number";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) rooms.add(map(rs));

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load rooms", e);
        }
        return rooms;
    }

    @Override
    public Room findById(int id) {
        String sql = "SELECT room_id, room_number, room_type, rate_per_night, max_guests, status, description, image_url " +
                     "FROM rooms WHERE room_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room by ID", e);
        }
    }

    @Override
    public Room findByNumber(String roomNumber) {
        String sql = "SELECT room_id, room_number, room_type, rate_per_night, max_guests, status, description, image_url " +
                     "FROM rooms WHERE room_number = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, roomNumber);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room by number", e);
        }
    }

    @Override
    public int create(Room room) {
        String sql = "INSERT INTO rooms (room_number, room_type, rate_per_night, max_guests, status, description, image_url) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getRoomType());
            ps.setDouble(3, room.getRatePerNight());
            ps.setInt(4, room.getMaxGuests());
            ps.setString(5, room.getStatus());
            ps.setString(6, room.getDescription());
            ps.setString(7, room.getImageUrl());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create room", e);
        }
    }

    @Override
    public boolean update(Room room) {
        String sql = "UPDATE rooms SET room_number=?, room_type=?, rate_per_night=?, max_guests=?, status=?, description=?, image_url=? " +
                     "WHERE room_id=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getRoomType());
            ps.setDouble(3, room.getRatePerNight());
            ps.setInt(4, room.getMaxGuests());
            ps.setString(5, room.getStatus());
            ps.setString(6, room.getDescription());
            ps.setString(7, room.getImageUrl());
            ps.setInt(8, room.getRoomId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update room", e);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM rooms WHERE room_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete room", e);
        }
    }

    @Override
    public double findPriceById(int roomId) {
        String sql = "SELECT rate_per_night FROM rooms WHERE room_id=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load room price", e);
        }
    }

    private Room map(ResultSet rs) throws SQLException {
        Room room = new Room();
        room.setRoomId(rs.getInt("room_id"));
        room.setRoomNumber(rs.getString("room_number"));
        room.setRoomType(rs.getString("room_type"));
        room.setRatePerNight(rs.getDouble("rate_per_night"));
        room.setMaxGuests(rs.getInt("max_guests"));
        room.setStatus(rs.getString("status"));
        room.setDescription(rs.getString("description"));
        room.setImageUrl(rs.getString("image_url"));
        return room;
    }
}
