package com.oceanview.dao;

import com.oceanview.dao.impl.UserDAOImpl;
import com.oceanview.model.User;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class UserDAOTest {

    private UserDAO userDAO;

    @BeforeEach
    void setup() {
        userDAO = new UserDAOImpl();
    }

    @Test
    void TEST_CASE_01_findByUsername_shouldReturnUser_whenExists() {
        // CHANGE this username to a real username in your DB
        String username = "admin";

        User u = userDAO.findByUsername(username);

        assertNotNull(u);
        assertEquals(username, u.getUsername());
    }

    @Test
    void TEST_CASE_02_findByUsername_shouldReturnNull_whenNotExists() {
        String username = "notfound_" + System.currentTimeMillis();

        User u = userDAO.findByUsername(username);

        assertNull(u);
    }
}
