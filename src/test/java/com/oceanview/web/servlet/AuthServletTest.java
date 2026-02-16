package com.oceanview.web.servlet;

import com.oceanview.model.User;
import com.oceanview.service.AuthService;
import com.oceanview.util.Flash;
import org.junit.jupiter.api.Test;

import javax.servlet.http.*;

import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

public class AuthServletTest {

    @Test
    void TEST_CASE_01_doPost_shouldRedirectToDashboard_whenAdminLoginSuccess() throws Exception {
        AuthServlet servlet = new AuthServlet();

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);

        when(req.getParameter("username")).thenReturn("admin");
        when(req.getParameter("password")).thenReturn("admin123");
        when(req.getContextPath()).thenReturn("/OceanViewResortBooking");
        when(req.getSession(true)).thenReturn(session);

        AuthService authServiceMock = mock(AuthService.class);

        User u = new User();
        u.setUserId(1);
        u.setUsername("admin");
        u.setRole("ADMIN");
        u.setStatus("ACTIVE");

        when(authServiceMock.login("admin", "admin123")).thenReturn(u);

        inject(servlet, "authService", authServiceMock);

        servlet.doPost(req, resp);

        verify(session).setAttribute("user", u);
        verify(session).setMaxInactiveInterval(60 * 60);
        verify(resp).addCookie(any(Cookie.class));
        verify(resp).sendRedirect("/OceanViewResortBooking/dashboard.html");
    }

    @Test
    void TEST_CASE_02_doPost_shouldRedirectToLogin_whenLoginFail() throws Exception {
        AuthServlet servlet = new AuthServlet();

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);

        when(req.getParameter("username")).thenReturn("wrong");
        when(req.getParameter("password")).thenReturn("wrong");
        when(req.getContextPath()).thenReturn("/OceanViewResortBooking");
        when(req.getSession(true)).thenReturn(session);

        AuthService authServiceMock = mock(AuthService.class);
        when(authServiceMock.login("wrong", "wrong")).thenReturn(null);

        inject(servlet, "authService", authServiceMock);

        servlet.doPost(req, resp);

        verify(resp).sendRedirect("/OceanViewResortBooking/login.html");
        verify(session, never()).setAttribute(eq("user"), any());

        // Flash.error stores message in session
        verify(session, atLeastOnce()).setAttribute(eq(Flash.KEY_ERROR), any());
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
