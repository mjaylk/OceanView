package com.oceanview.dao;

import com.oceanview.model.Guest;
import java.util.List;

public interface GuestDAO {

    // dao layer
    // abstraction

    List<Guest> findAll();               // read all
    Guest findById(int id);             // read by id
    
    // validation support
    Guest findByEmail(String email);   
    Guest findByContactNumber(String contactNumber);

    List<Guest> search(String q, int limit); // search 

    int getNextGuestId();               // id generation
    int create(Guest guest);            // create
    boolean update(Guest guest);        // update
    boolean delete(int id);             // delete

    // security
    boolean updatePassword(int guestId, String password);       
    boolean updatePasswordById(int guestId, String password);   

    Guest findByEmailAndPassword(String email, String password); // login
}
