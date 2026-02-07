package com.oceanview.dao;

import com.oceanview.model.User;
import java.util.List;

public interface UserDAO {


    // abstraction

    User findByUsername(String username);   // login lookup
    User findById(int id);                  // read by id
    List<User> findAll();                   // read all

    int create(User user);                  // create
    boolean update(User user);              // update
    boolean updatePassword(int id, String passwordHash); // security
    boolean deactivate(int id);             // deactivate
    boolean delete(int id);                 //  delete
}
