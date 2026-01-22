package com.oceanview.service;

import com.oceanview.dao.UserDAO;
import com.oceanview.dao.impl.UserDAOImpl;
import com.oceanview.model.User;
import com.oceanview.util.PasswordUtil;

public class AuthService {

    private final UserDAO userDAO = new UserDAOImpl();

    public User login(String username, String password) {
    	
    	// Checking AuthSerivice hit 

        System.out.println(">>> AuthService.login HIT (TestCase 01) <<<");
//        System.out.println("RAW username=[" + username + "]");
//        System.out.println("RAW password=[" + password + "]");

        if (username == null || password == null) return null;

        username = username.trim();
        password = password.trim();

        System.out.println("TRIM username=[" + username + "]");
        System.out.println("TRIM password=[" + password + "]");

        User user = userDAO.findByUsername(username);

        System.out.println("USER from DB = " + (user == null ? "null" : "FOUND"));
        if (user != null) {
            System.out.println("DB STATUS = [" + user.getStatus() + "]");
            System.out.println("DB HASH   = [" + user.getPasswordHash() + "]");
        }

        if (user == null) return null;
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) return null;

        boolean ok = PasswordUtil.verifyPassword(password, user.getPasswordHash());
        System.out.println("VERIFY = " + ok);

        return ok ? user : null;
    }


}
