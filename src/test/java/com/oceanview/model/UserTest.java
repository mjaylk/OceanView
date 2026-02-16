package com.oceanview.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    void TEST_CASE_01_settersAndGetters_shouldReturnSameValues() {
        User u = new User();

        u.setUserId(7);
        u.setUsername("vishwa");
        u.setPasswordHash("hash_123");
        u.setRole("ADMIN");
        u.setStatus("ACTIVE");

        assertEquals(7, u.getUserId());
        assertEquals("vishwa", u.getUsername());
        assertEquals("hash_123", u.getPasswordHash());
        assertEquals("ADMIN", u.getRole());
        assertEquals("ACTIVE", u.getStatus());
    }

    @Test
    void TEST_CASE_02_defaultValues_shouldBeNullOrZero() {
        User u = new User();

        assertEquals(0, u.getUserId());      
        assertNull(u.getUsername());         
        assertNull(u.getPasswordHash());
        assertNull(u.getRole());
        assertNull(u.getStatus());
    }

    @Test
    void TEST_CASE_03_overwriteValues_shouldUpdateCorrectly() {
        User u = new User();

        u.setUserId(1);
        u.setUsername("oldUser");
        u.setRole("USER");
        u.setStatus("ACTIVE");

        // update values
        u.setUserId(2);
        u.setUsername("newUser");
        u.setRole("ADMIN");
        u.setStatus("INACTIVE");

        assertEquals(2, u.getUserId());
        assertEquals("newUser", u.getUsername());
        assertEquals("ADMIN", u.getRole());
        assertEquals("INACTIVE", u.getStatus());
    }
}
