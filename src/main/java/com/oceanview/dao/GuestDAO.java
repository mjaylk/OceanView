package com.oceanview.dao;

import com.oceanview.model.Guest;
import java.util.List;

public interface GuestDAO {
    List<Guest> findAll();
    Guest findById(int id);
    Guest findByEmail(String email);
    Guest findByContactNumber(String contactNumber);
  
  


    List<Guest> search(String q, int limit);

    int getNextGuestId();
    int create(Guest guest);
    boolean update(Guest guest);
    boolean delete(int id);
    boolean updatePassword(int guestId, String password);
    boolean updatePasswordById(int guestId, String password);

  
    Guest findByEmailAndPassword(String email, String password);
}
