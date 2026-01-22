package com.oceanview.service;

import com.oceanview.dao.UserDAO;
import com.oceanview.dao.impl.UserDAOImpl;
import com.oceanview.model.User;
import com.oceanview.util.DatabaseConnection;
import com.oceanview.util.PasswordUtil;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class UserService {

    private final UserDAO userDAO = new UserDAOImpl();

    public List<User> listUsers() {
        System.out.println("=== UserService.listUsers() called ===");
        List<User> users = userDAO.findAll();
        System.out.println("Found " + users.size() + " users:");
        for (User u : users) {
            System.out.println("  - ID=" + u.getUserId() + ", Username=" + u.getUsername() + 
                             ", Role=" + u.getRole() + ", Status=" + u.getStatus());
        }
        System.out.println("=== UserService.listUsers() END ===");
        return users;
    }

    public int createUser(String username, String plainPassword, String role) {
        System.out.println("=== UserService.createUser() ===");
        System.out.println("Input: username=" + username + ", role=" + role);
        
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username required");
        if (plainPassword == null || plainPassword.isBlank()) throw new IllegalArgumentException("Password required");
        if (role == null || role.isBlank()) throw new IllegalArgumentException("Role required");

        User existing = userDAO.findByUsername(username.trim());
        if (existing != null) {
            System.out.println("Username exists: " + existing.getUserId());
            throw new IllegalArgumentException("Username already exists");
        }

        User u = new User();
        u.setUsername(username.trim());
        String hash = PasswordUtil.hashPassword(plainPassword.trim());
        u.setPasswordHash(hash);
        System.out.println("Password hash: " + hash.substring(0, 20) + "...");
        u.setRole(role.trim().toUpperCase());
        u.setStatus("ACTIVE");

        int id = userDAO.create(u);
        System.out.println("Created user ID: " + id);
        System.out.println("=== UserService.createUser() END ===");
        return id;
    }

    public boolean updateUser(int id, String username, String role, String status) {
        System.out.println("=== UserService.updateUser(" + id + ") ===");
        System.out.println("New: username=" + username + ", role=" + role + ", status=" + status);
        
        User u = userDAO.findById(id);
        if (u == null) {
            System.out.println("User ID " + id + " not found");
            throw new IllegalArgumentException("User not found");
        }
        System.out.println("Found user: " + u.getUsername());

        u.setUsername(username.trim());
        u.setRole(role.trim().toUpperCase());
        u.setStatus(status.trim().toUpperCase());
        
        boolean result = userDAO.update(u);
        System.out.println("Update result: " + result);
        System.out.println("=== UserService.updateUser() END ===");
        return result;
    }

    public boolean resetPassword(int id, String newPlainPassword) {
        System.out.println("=== UserService.resetPassword(" + id + ") ===");
        if (newPlainPassword == null || newPlainPassword.isBlank())
            throw new IllegalArgumentException("Password required");

        String hash = PasswordUtil.hashPassword(newPlainPassword.trim());
        System.out.println("New password hash: " + hash.substring(0, 20) + "...");
        boolean result = userDAO.updatePassword(id, hash);
        System.out.println("Reset result: " + result);
        System.out.println("=== UserService.resetPassword() END ===");
        return result;
    }

    public boolean deactivateUser(int id) {
        System.out.println("=== UserService.deactivateUser(" + id + ") ===");
        boolean result = userDAO.deactivate(id);
        System.out.println("Deactivate result: " + result);
        System.out.println("=== UserService.deactivateUser() END ===");
        return result;
    }
    
    public boolean deleteUser(int id) {
        System.out.println("UserService.deleteUser(" + id + ")");
        return userDAO.delete(id);
    }


}
