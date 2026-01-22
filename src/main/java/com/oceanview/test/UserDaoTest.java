package com.oceanview.test;

import com.oceanview.dao.UserDAO;
import com.oceanview.dao.impl.UserDAOImpl;
import com.oceanview.model.User;

/**
 * Simple standalone test to verify:
 * - Database connection
 * - UserDAO SQL correctness
 * - Column mappings
 *
 * This class is NOT part of the web app.
 * It is used only for development/testing.
 */
public class UserDaoTest {

    public static void main(String[] args) {

        UserDAO userDAO = new UserDAOImpl();

        System.out.println("=== Fetch user by username ===");

        User user = userDAO.findByUsername("admin");

        if (user == null) {
            System.out.println("❌ User NOT found");
        } else {
            System.out.println("✅ User FOUND");
            System.out.println("ID      : " + user.getUserId());
            System.out.println("Username: " + user.getUsername());
            System.out.println("Role    : " + user.getRole());
            System.out.println("Status  : " + user.getStatus());
            System.out.println("Hash    : " + user.getPasswordHash());
        }
    }
}
