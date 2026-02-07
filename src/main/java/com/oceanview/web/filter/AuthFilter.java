package com.oceanview.web.filter;

import com.oceanview.model.User;
import com.oceanview.model.Guest;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {

    // check path start
    private boolean startsWith(String path, String... prefixes) {
        for (String p : prefixes) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    // admin check
    private boolean isAdmin(User u) {
        return u != null && "ADMIN".equalsIgnoreCase(u.getRole());
    }

    // staff or admin check
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

        // public pages
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

        User staffUser = null;
        Guest guestUser = null;

        // session read
        if (session != null) {
            Object u = session.getAttribute("user");
            if (u instanceof User) staffUser = (User) u;

            Object g = session.getAttribute("guest");
            if (g instanceof Guest) guestUser = (Guest) g;
        }

        // guest pages
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

        // admin only pages
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

        // staff and admin pages
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

        // default rule
        if (staffUser == null) {
            resp.sendRedirect(ctx + "/login.html");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
