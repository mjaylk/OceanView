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

    public int createRoom(String roomNumber, String roomType, double ratePerNight, String status,int maxGuests) {
        if (roomDAO.findByNumber(roomNumber) != null) {
            throw new IllegalArgumentException("Room number already exists");
        }
        
        Room room = new Room(roomNumber, roomType, ratePerNight, status, maxGuests);
        return roomDAO.create(room);
    }

    public boolean updateRoom(int id, String roomNumber, String roomType, double ratePerNight, String status , int maxGuests) {
        Room existing = roomDAO.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Room not found");
        }
        
        Room room = new Room(roomNumber, roomType, ratePerNight, status, maxGuests);
        room.setRoomId(id);
        return roomDAO.update(room);
    }

    public boolean deleteRoom(int id) {
        return roomDAO.delete(id);
    }
}
