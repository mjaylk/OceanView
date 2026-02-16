package com.oceanview.service;

import com.oceanview.dao.UserDAO;
import com.oceanview.model.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    @Test
    void login_wrongUsername_returnsNull() throws Exception {
        AuthService service = new AuthService();

        UserDAO userDAO = mock(UserDAO.class);
        when(userDAO.findByUsername("wrong")).thenReturn(null);

   
        inject(service, "userDAO", userDAO);

        User result = service.login("wrong", "123");

        assertNull(result);
        verify(userDAO).findByUsername("wrong");
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
