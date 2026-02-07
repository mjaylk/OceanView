package com.oceanview.dao;

import com.oceanview.model.Room;
import java.util.List;

public interface RoomDAO {

    
    // abstraction

    List<Room> findAll();                // read all
    Room findById(int id);               // read by id
    Room findByNumber(String roomNumber); // business key

    int create(Room room);               // create
    boolean update(Room room);           // update
    boolean delete(int id);              // delete

    double findPriceById(int roomId);    // pricing logic
    boolean updateStatus(int roomId, String status); // update room status
}
