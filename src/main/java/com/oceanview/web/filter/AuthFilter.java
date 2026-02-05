package com.oceanview.web.filter;

import com.oceanview.model.User;
import com.oceanview.model.Guest;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {

    private boolean startsWith(String path, String... prefixes) {
        for (String p : prefixes) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    private boolean isAdmin(User u) {
        return u != null && "ADMIN".equalsIgnoreCase(u.getRole());
    }

    private boolean isStaffOrAdmin(User u) {
        if (u == null) return false;
        String r = u.getRole();
        return "ADMIN".equalsIgnoreCase(r) || "STAFF".equalsIgnoreCase(r);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String ctx = req.getContextPath();
        String uri = req.getRequestURI();
        String path = uri.substring(ctx.length());

        /* 
           Public resources must bypass, otherwise you get redirect loops.
           This includes guest-login.html, because guest users do not have "user" session.
        */
        if (
                path.equals("/login.html") ||
                path.equals("/guest-login.html") ||
                path.equals("/index.html") ||
                path.equals("/") ||
                path.equals("/api/login") ||
                path.equals("/api/logout") ||
                path.equals("/api/guest/login") ||
                path.equals("/api/guest/logout") ||
                startsWith(path, "/assets/", "/partials/")
        ) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);

        /* 
           Two session types:
           - staff/admin session: session attribute name "user"
           - guest session: session attribute name "guest"
        */
        User staffUser = null;
        Guest guestUser = null;

        if (session != null) {
            Object u = session.getAttribute("user");
            if (u instanceof User) staffUser = (User) u;

            Object g = session.getAttribute("guest");
            if (g instanceof Guest) guestUser = (Guest) g;
        }

        /* 
           Guest pages should be protected by "guest" session.
           If guest not logged in, redirect to guest-login.html (not login.html).
        */
        if (
                path.equals("/guest-reservations.html") ||
                path.startsWith("/api/guest/reservations")
        ) {
            if (guestUser == null) {
                resp.sendRedirect(ctx + "/guest-login.html");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        /*
           Admin-only pages.
           If staff tries, send them to dashboard.
           If not logged in, send to login.html.
        */
        if (
                path.equals("/users.html") ||
                path.startsWith("/api/users")
        ) {
            if (staffUser == null) {
                resp.sendRedirect(ctx + "/login.html");
                return;
            }
            if (!isAdmin(staffUser)) {
                resp.sendRedirect(ctx + "/dashboard.html");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        /*
           Staff + Admin pages.
        */
        if (
                path.equals("/dashboard.html") ||
                path.equals("/reservation.html") ||
                path.equals("/rooms.html") ||
                path.equals("/guests.html") ||
                path.startsWith("/api/reservations") ||
                path.startsWith("/api/rooms") ||
                path.startsWith("/api/guests") ||
                path.equals("/api/me")
        ) {
            if (!isStaffOrAdmin(staffUser)) {
                resp.sendRedirect(ctx + "/login.html");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        /*
           Default rule:
           If it is not in any allowed list, require staff/admin login.
           This prevents random pages from being opened without authentication.
        */
        if (staffUser == null) {
            resp.sendRedirect(ctx + "/login.html");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override public void init(FilterConfig filterConfig) {}
    @Override public void destroy() {}
}
