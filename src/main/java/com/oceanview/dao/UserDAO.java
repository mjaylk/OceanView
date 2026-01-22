package com.oceanview.dao;

import com.oceanview.model.User;
import java.util.List;

public interface UserDAO {
    User findByUsername(String username);
    User findById(int id);

    List<User> findAll();

    int create(User user);          
    boolean update(User user);     
    boolean updatePassword(int id, String passwordHash);
    boolean deactivate(int id);  
}
