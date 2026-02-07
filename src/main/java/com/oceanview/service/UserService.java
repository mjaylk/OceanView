package com.oceanview.service;

import com.oceanview.dao.UserDAO;
import com.oceanview.dao.impl.UserDAOImpl;
import com.oceanview.model.User;
import com.oceanview.util.PasswordUtil;

import java.util.List;

public class UserService {

    // DAO object
    private final UserDAO userDAO = new UserDAOImpl();

    // Get all users
    public List<User> listUsers() {
        return userDAO.findAll();
    }

    // Create new user
    public int createUser(String username, String plainPassword, String role) {

        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username required");

        if (plainPassword == null || plainPassword.isBlank())
            throw new IllegalArgumentException("Password required");

        if (role == null || role.isBlank())
            throw new IllegalArgumentException("Role required");

        User existing = userDAO.findByUsername(username.trim());
        if (existing != null)
            throw new IllegalArgumentException("Username already exists");

        User u = new User();
        u.setUsername(username.trim());
        u.setPasswordHash(PasswordUtil.hashPassword(plainPassword.trim()));
        u.setRole(role.trim().toUpperCase());
        u.setStatus("ACTIVE");

        return userDAO.create(u);
    }

    // Update user details
    public boolean updateUser(int id, String username, String role, String status) {

        User u = userDAO.findById(id);
        if (u == null)
            throw new IllegalArgumentException("User not found");

        u.setUsername(username.trim());
        u.setRole(role.trim().toUpperCase());
        u.setStatus(status.trim().toUpperCase());

        return userDAO.update(u);
    }

    // Reset user password
    public boolean resetPassword(int id, String newPlainPassword) {

        if (newPlainPassword == null || newPlainPassword.isBlank())
            throw new IllegalArgumentException("Password required");

        String hash = PasswordUtil.hashPassword(newPlainPassword.trim());
        return userDAO.updatePassword(id, hash);
    }

    // Deactivate user
    public boolean deactivateUser(int id) {
        return userDAO.deactivate(id);
    }

    // Delete user
    public boolean deleteUser(int id) {
        return userDAO.delete(id);
    }

    // Simple manual test
    public static void main(String[] args) {

        UserService service = new UserService();

        System.out.println("TEST CASE 01 - Create user with valid data");

        try {
            int userId = service.createUser(
                    "test_user_01",
                    "password123",
                    "ADMIN"
            );

            if (userId > 0) {
                System.out.println("RESULT: PASS");
            } else {
                System.out.println("RESULT: FAIL");
            }

        } catch (Exception e) {
            System.out.println("RESULT: FAIL");
            System.out.println("Error: " + e.getMessage());
        }
    }
}
