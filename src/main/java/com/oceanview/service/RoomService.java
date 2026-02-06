package com.oceanview.service;

import com.oceanview.dao.RoomDAO;
import com.oceanview.dao.impl.RoomDAOImpl;
import com.oceanview.model.Room;

import java.util.List;

public class RoomService {

    private final RoomDAO roomDAO = new RoomDAOImpl();

    public List<Room> listRooms() {
        return roomDAO.findAll();
    }

    public Room getRoom(int id) {
        return roomDAO.findById(id);
    }

    public int createRoom(String roomNumber, String roomType, double ratePerNight,
                          String status, int maxGuests, String description, String imageUrl) {

        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Room number is required");
        }

        if (roomDAO.findByNumber(roomNumber.trim()) != null) {
            throw new IllegalArgumentException("Room number already exists");
        }

        Room room = new Room(roomNumber.trim(), roomType, ratePerNight, status, maxGuests);
        room.setDescription(description);
        room.setImageUrl(imageUrl);

        return roomDAO.create(room);
    }

    public boolean updateRoom(int id, String roomNumber, String roomType, double ratePerNight,
                              String status, int maxGuests, String description, String imageUrl) {

        Room existing = roomDAO.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Room not found");
        }

        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Room number is required");
        }

        // If room number is changing, check duplicates
        Room byNumber = roomDAO.findByNumber(roomNumber.trim());
        if (byNumber != null && byNumber.getRoomId() != id) {
            throw new IllegalArgumentException("Room number already exists");
        }

        Room room = new Room(roomNumber.trim(), roomType, ratePerNight, status, maxGuests);
        room.setRoomId(id);
        room.setDescription(description);
        room.setImageUrl(imageUrl);

        return roomDAO.update(room);
    }

    public boolean deleteRoom(int id) {
        return roomDAO.delete(id);
    }
}
