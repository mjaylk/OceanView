package com.oceanview.service;

import com.oceanview.dao.UserDAO;
import com.oceanview.dao.impl.UserDAOImpl;
import com.oceanview.model.User;
import com.oceanview.util.PasswordUtil;

public class AuthService {

    // dao object
    private final UserDAO userDAO = new UserDAOImpl();

    // login user
    public User login(String username, String password) {

        if (username == null || password == null) return null;

        username = username.trim();
        password = password.trim();

        User user = userDAO.findByUsername(username);

        if (user == null) return null;

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) return null;

        boolean ok = PasswordUtil.verifyPassword(password, user.getPasswordHash());

        return ok ? user : null;
    }

    // simple manual test
    public static void main(String[] args) {

        AuthService service = new AuthService();

        System.out.println("TEST CASE 01 - Login with invalid user");

        User user = service.login("wrong_user", "wrong_pass");

        if (user == null) {
            System.out.println("RESULT: PASS");
        } else {
            System.out.println("RESULT: FAIL");
        }
    }
}
