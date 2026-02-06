package com.oceanview.web.servlet;

import com.oceanview.model.Setting;
import com.oceanview.model.User;
import com.oceanview.service.SettingService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/api/settings/*")
public class SettingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private SettingService service;
    
    @Override
    public void init() {
        service = new SettingService();
    }
    
    private User getSessionUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        return (User) session.getAttribute("user");
    }
    
    private boolean isAdmin(HttpServletRequest req) {
        User user = getSessionUser(req);
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }
    
    private void sendJson(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(json);
    }
    
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!isAdmin(req)) {
            sendJson(resp, 403, "{\"success\":false,\"message\":\"Admin access required\"}");
            return;
        }
        
        String path = req.getPathInfo();
        
        if (path == null || "/".equals(path)) {
            List<Setting> settings = service.getAllSettings();
            
            StringBuilder sb = new StringBuilder("{\"success\":true,\"settings\":[");
            for (int i = 0; i < settings.size(); i++) {
                Setting s = settings.get(i);
                sb.append("{")
                  .append("\"settingId\":").append(s.getSettingId()).append(",")
                  .append("\"settingKey\":\"").append(esc(s.getSettingKey())).append("\",")
                  .append("\"settingValue\":\"").append(esc(s.getSettingValue())).append("\",")
                  .append("\"settingCategory\":\"").append(esc(s.getSettingCategory())).append("\",")
                  .append("\"description\":\"").append(esc(s.getDescription())).append("\"")
                  .append("}");
                if (i < settings.size() - 1) sb.append(",");
            }
            sb.append("]}");
            
            sendJson(resp, 200, sb.toString());
            return;
        }
        
        if ("/by-category".equals(path)) {
            String category = req.getParameter("category");
            if (category == null || category.trim().isEmpty()) {
                sendJson(resp, 400, "{\"success\":false,\"message\":\"Category is required\"}");
                return;
            }
            
            try {
                List<Setting> settings = service.getSettingsByCategory(category);
                
                StringBuilder sb = new StringBuilder("{\"success\":true,\"settings\":[");
                for (int i = 0; i < settings.size(); i++) {
                    Setting s = settings.get(i);
                    sb.append("{")
                      .append("\"settingKey\":\"").append(esc(s.getSettingKey())).append("\",")
                      .append("\"settingValue\":\"").append(esc(s.getSettingValue())).append("\",")
                      .append("\"description\":\"").append(esc(s.getDescription())).append("\"")
                      .append("}");
                    if (i < settings.size() - 1) sb.append(",");
                }
                sb.append("]}");
                
                sendJson(resp, 200, sb.toString());
                
            } catch (Exception e) {
                sendJson(resp, 400, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
            }
            return;
        }
        
        sendJson(resp, 404, "{\"success\":false,\"message\":\"Endpoint not found\"}");
    }
    
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!isAdmin(req)) {
            sendJson(resp, 403, "{\"success\":false,\"message\":\"Admin access required\"}");
            return;
        }
        
        User user = getSessionUser(req);
        if (user == null) {
            sendJson(resp, 401, "{\"success\":false,\"message\":\"Session expired\"}");
            return;
        }
        
        String body = getBody(req);
        
        try {
            Map<String, String> settings = parseSettings(body);
            
            if (settings.isEmpty()) {
                sendJson(resp, 400, "{\"success\":false,\"message\":\"No settings provided\"}");
                return;
            }
            
            boolean success = service.updateMultipleSettings(settings, user.getUserId());
            
            if (success) {
                sendJson(resp, 200, "{\"success\":true,\"message\":\"Settings updated successfully\"}");
            } else {
                sendJson(resp, 400, "{\"success\":false,\"message\":\"Failed to update settings\"}");
            }
            
        } catch (Exception e) {
            sendJson(resp, 400, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
    }
    
    private String getBody(HttpServletRequest req) throws IOException {
        java.util.Scanner s = new java.util.Scanner(req.getInputStream()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
    
    private Map<String, String> parseSettings(String json) {
        Map<String, String> settings = new java.util.HashMap<>();
        
        if (json == null || json.trim().isEmpty()) {
            return settings;
        }
        
        json = json.trim();
        if (json.startsWith("{")) {
            json = json.substring(1);
        }
        if (json.endsWith("}")) {
            json = json.substring(0, json.length() - 1);
        }
        
        String[] pairs = json.split(",(?=\\s*\")");
        
        for (String pair : pairs) {
            int colonIndex = pair.indexOf(":");
            if (colonIndex == -1) continue;
            
            String key = pair.substring(0, colonIndex).trim();
            String value = pair.substring(colonIndex + 1).trim();
            
            key = key.replaceAll("^\"|\"$", "").trim();
            value = value.replaceAll("^\"|\"$", "").trim();
            
            if (!key.isEmpty()) {
                settings.put(key, value);
            }
        }
        
        return settings;
    }
}