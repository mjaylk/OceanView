package com.oceanview.dao;

import com.oceanview.model.Room;
import java.util.List;

public interface RoomDAO {
    List<Room> findAll();
    Room findById(int id);
    Room findByNumber(String roomNumber);
    int create(Room room);
    boolean update(Room room);
    boolean delete(int id);
}
