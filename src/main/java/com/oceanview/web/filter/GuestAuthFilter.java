package com.oceanview.web.filter;

import com.oceanview.model.Guest;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;

@WebFilter({"/guest-reservations.html", "/api/guest/*"})
public class GuestAuthFilter implements Filter {

    private boolean bypass(String path) {
        return path.equals("/api/guest/login")
                || path.equals("/api/guest/logout");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String ctx = req.getContextPath();
        String uri = req.getRequestURI();
        String path = uri.substring(ctx.length());

        if (bypass(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        Guest guest = (session != null) ? (Guest) session.getAttribute("guest") : null;

        if (guest == null) {
            if (path.endsWith(".html")) {
                resp.sendRedirect(ctx + "/guest-login.html");
                return;
            }
            resp.setStatus(401);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"success\":false,\"message\":\"Guest login required\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override public void init(FilterConfig filterConfig) {}
    @Override public void destroy() {}
}
