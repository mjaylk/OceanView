package com.oceanview.web.filter;

import com.oceanview.model.User;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class AuthFilter implements Filter {  

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);

        User user = (User) (session != null ? session.getAttribute("user") : null);
        
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            System.out.println("AuthFilter BLOCKED: " + req.getRequestURI());
            resp.sendRedirect(req.getContextPath() + "/login.html?unauthorized=1");
            return;
        }

        System.out.println("AuthFilter PASSED: " + user.getUsername());
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) {}
    @Override
    public void destroy() {}
}
