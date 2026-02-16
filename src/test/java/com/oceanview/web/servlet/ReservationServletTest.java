package com.oceanview.web.servlet;

import com.oceanview.model.User;
import com.oceanview.service.ReservationService;
import com.oceanview.util.Flash;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletInputStream;
import javax.servlet.http.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

public class ReservationServletTest {

    @Test
    void TEST_CASE_01_doGet_stats_shouldReturnJson_whenStaff() throws Exception {
        ReservationServlet servlet = new ReservationServlet();

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);

        // staff/admin session
        User staff = new User();
        staff.setUserId(1);
        staff.setRole("STAFF");

        when(req.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(staff);

        when(req.getPathInfo()).thenReturn("/stats");
        when(req.getParameter("days")).thenReturn("7");

        // response writer
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        when(resp.getWriter()).thenReturn(pw);

        ReservationService serviceMock = mock(ReservationService.class);
        when(serviceMock.getDashboardStatsJson(7)).thenReturn("{\"success\":true}");

        inject(servlet, "service", serviceMock);

        servlet.doGet(req, resp);

        pw.flush();

        verify(resp).setStatus(200);
        verify(resp).setContentType("application/json;charset=UTF-8");
    }

    @Test
    void TEST_CASE_02_doPost_shouldCreateReservation_andReturn201_whenStaff() throws Exception {
        ReservationServlet servlet = new ReservationServlet();

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);

        // staff/admin session
        User admin = new User();
        admin.setUserId(10);
        admin.setRole("ADMIN");

        when(req.getSession(false)).thenReturn(session);
        when(req.getSession(true)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(admin);

        // body JSON (must match keys your servlet extracts)
        String body =
                "{" +
                        "\"guestId\":0," +
                        "\"guestName\":\"John\"," +
                        "\"guestEmail\":\"john@email.com\"," +
                        "\"guestContactNumber\":\"0771234567\"," +
                        "\"roomId\":1," +
                        "\"checkInDate\":\"2026-02-20\"," +
                        "\"checkOutDate\":\"2026-02-22\"," +
                        "\"status\":\"CONFIRMED\"," +
                        "\"notes\":\"test\"," +
                        "\"taxRate\":0," +
                        "\"discount\":0" +
                "}";

        when(req.getInputStream()).thenReturn(new MockServletInputStream(body));

        // response writer
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        when(resp.getWriter()).thenReturn(pw);

        ReservationService serviceMock = mock(ReservationService.class);
        when(serviceMock.createReservationSmart(
                anyInt(), anyString(), anyString(), anyString(),
                isNull(), // guestPassword is null in servlet
                anyInt(), any(java.sql.Date.class), any(java.sql.Date.class),
                anyString(), anyString(),
                anyDouble(), anyDouble(),
                eq(10) // user.getUserId()
        )).thenReturn(55);

        inject(servlet, "service", serviceMock);

        servlet.doPost(req, resp);

        pw.flush();

        verify(resp).setStatus(201);
        verify(resp).setContentType("application/json;charset=UTF-8");

        // flash success
        verify(session, atLeastOnce()).setAttribute(eq(Flash.KEY_SUCCESS), any());
    }

    @Test
    void TEST_CASE_03_doPost_shouldReturn400_whenServiceThrows() throws Exception {
        ReservationServlet servlet = new ReservationServlet();

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);

        User admin = new User();
        admin.setUserId(10);
        admin.setRole("ADMIN");

        when(req.getSession(false)).thenReturn(session);
        when(req.getSession(true)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(admin);

        // invalid JSON (missing email -> service may throw)
        String body =
                "{" +
                        "\"guestId\":0," +
                        "\"guestName\":\"John\"," +
                        "\"guestEmail\":\"\"," +
                        "\"guestContactNumber\":\"0771234567\"," +
                        "\"roomId\":1," +
                        "\"checkInDate\":\"2026-02-20\"," +
                        "\"checkOutDate\":\"2026-02-22\"" +
                "}";

        when(req.getInputStream()).thenReturn(new MockServletInputStream(body));

        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        when(resp.getWriter()).thenReturn(pw);

        ReservationService serviceMock = mock(ReservationService.class);
        when(serviceMock.createReservationSmart(
                anyInt(), anyString(), anyString(), anyString(),
                isNull(),
                anyInt(), any(java.sql.Date.class), any(java.sql.Date.class),
                anyString(), anyString(),
                anyDouble(), anyDouble(),
                eq(10)
        )).thenThrow(new IllegalArgumentException("Guest email required"));

        inject(servlet, "service", serviceMock);

        servlet.doPost(req, resp);

        pw.flush();

        verify(resp).setStatus(400);
        verify(resp).setContentType("application/json;charset=UTF-8");

        // flash error
        verify(session, atLeastOnce()).setAttribute(eq(Flash.KEY_ERROR), any());
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    // Minimal ServletInputStream for mocking request body
    private static class MockServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream in;

        MockServletInputStream(String data) {
            this.in = new ByteArrayInputStream(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        @Override
        public int read() throws IOException {
            return in.read();
        }

        @Override
        public boolean isFinished() {
            return in.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(javax.servlet.ReadListener readListener) {
            // not needed for this test
        }
    }
}
