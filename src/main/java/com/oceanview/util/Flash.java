package com.oceanview.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class Flash {

    public static final String KEY_SUCCESS = "FLASH_SUCCESS";
    public static final String KEY_ERROR = "FLASH_ERROR";

    public static void success(HttpServletRequest req, String message) {
        HttpSession session = req.getSession(true);
        // Clear any existing flash messages first
        session.removeAttribute(KEY_ERROR);
        session.setAttribute(KEY_SUCCESS, message);
    }

    public static void error(HttpServletRequest req, String message) {
        HttpSession session = req.getSession(true);
        // Clear any existing flash messages first
        session.removeAttribute(KEY_SUCCESS);
        session.setAttribute(KEY_ERROR, message);
    }
    
    // Optional: Clear all flash messages
    public static void clear(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.removeAttribute(KEY_SUCCESS);
            session.removeAttribute(KEY_ERROR);
        }
    }
}