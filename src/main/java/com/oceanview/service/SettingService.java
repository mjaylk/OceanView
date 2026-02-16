package com.oceanview.service;

import com.oceanview.dao.SettingDAO;
import com.oceanview.dao.impl.SettingDAOImpl;
import com.oceanview.model.Setting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingService {

    // DAO object
    private final SettingDAO dao = new SettingDAOImpl();

    // Get all settings
    public List<Setting> getAllSettings() {
        return dao.findAll();
    }

    // Get settings by category
    public List<Setting> getSettingsByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category is required");
        }
        return dao.findByCategory(category.toUpperCase());
    }

    // Convert settings list to key-value map
    public Map<String, String> getSettingsAsMap(String category) {
        List<Setting> settings = getSettingsByCategory(category);
        Map<String, String> map = new HashMap<>();

        for (Setting s : settings) {
            map.put(s.getSettingKey(), s.getSettingValue());
        }
        return map;
    }

    // Get setting value by key
    public String getValue(String key) {
        if (key == null || key.trim().isEmpty()) return null;

        Setting s = dao.findByKey(key.trim());
        if (s == null) return null;

        String v = s.getSettingValue();
        return v == null ? null : v.trim();
    }

    // Get value or default
    public String getValueOrDefault(String key, String def) {
        String v = getValue(key);
        return (v == null || v.isEmpty()) ? def : v;
    }

    // Get setting value
    public String getSetting(String key) {
        return dao.getValue(key);
    }

    // Get setting value with default
    public String getSetting(String key, String defaultValue) {
        return dao.getValue(key, defaultValue);
    }

    // Update one setting
    public boolean updateSetting(String key, String value, int updatedBy) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Setting key is required");
        }
        if (updatedBy <= 0) {
            throw new IllegalArgumentException("Invalid user");
        }
        return dao.update(key, value, updatedBy);
    }

    // Update multiple settings
    public boolean updateMultipleSettings(Map<String, String> settings, int updatedBy) {
        if (settings == null || settings.isEmpty()) {
            throw new IllegalArgumentException("Settings are required");
        }
        if (updatedBy <= 0) {
            throw new IllegalArgumentException("Invalid user");
        }

        List<Setting> settingList = new java.util.ArrayList<>();

        for (Map.Entry<String, String> entry : settings.entrySet()) {
            Setting s = new Setting();
            s.setSettingKey(entry.getKey());
            s.setSettingValue(entry.getValue());
            settingList.add(s);
        }

        return dao.updateMultiple(settingList, updatedBy);
    }

    // Get SMTP settings
    public Map<String, String> getSmtpSettings() {
        return getSettingsAsMap("EMAIL");
    }

    // Test SMTP settings values
    public boolean testSmtpConnection(Map<String, String> smtpSettings) {

        String host = smtpSettings.get("smtp_host");
        String port = smtpSettings.get("smtp_port");

        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("SMTP host is required");
        }
        if (port == null || port.trim().isEmpty()) {
            throw new IllegalArgumentException("SMTP port is required");
        }

        try {
            Integer.parseInt(port);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("SMTP port must be a number");
        }

        return true;
    }

 
}
