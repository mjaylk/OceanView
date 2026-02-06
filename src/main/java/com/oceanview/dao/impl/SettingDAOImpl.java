package com.oceanview.dao.impl;

import com.oceanview.dao.SettingDAO;
import com.oceanview.model.Setting;
import com.oceanview.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SettingDAOImpl implements SettingDAO {

    @Override
    public List<Setting> findAll() {
        String sql = "SELECT setting_id, setting_key, setting_value, setting_category, description, updated_at, updated_by " +
                     "FROM settings ORDER BY setting_category, setting_key";
        
        List<Setting> list = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load settings", e);
        }
    }

    @Override
    public List<Setting> findByCategory(String category) {
        String sql = "SELECT setting_id, setting_key, setting_value, setting_category, description, updated_at, updated_by " +
                     "FROM settings WHERE setting_category = ? ORDER BY setting_key";
        
        List<Setting> list = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, category);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load settings by category", e);
        }
    }

    @Override
    public Setting findByKey(String key) {
        String sql = "SELECT setting_id, setting_key, setting_value, setting_category, description, updated_at, updated_by " +
                     "FROM settings WHERE setting_key = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, key);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find setting by key", e);
        }
    }

    @Override
    public String getValue(String key) {
        return getValue(key, null);
    }

    @Override
    public String getValue(String key, String defaultValue) {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, key);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String value = rs.getString("setting_value");
                    return value != null ? value : defaultValue;
                }
                return defaultValue;
            }
            
        } catch (SQLException e) {
            return defaultValue;
        }
    }

    @Override
    public boolean update(String key, String value, int updatedBy) {
        String sql = "UPDATE settings SET setting_value = ?, updated_by = ? WHERE setting_key = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, value);
            ps.setInt(2, updatedBy);
            ps.setString(3, key);
            
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update setting", e);
        }
    }

    @Override
    public boolean updateMultiple(List<Setting> settings, int updatedBy) {
        String sql = "UPDATE settings SET setting_value = ?, updated_by = ? WHERE setting_key = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            for (Setting setting : settings) {
                ps.setString(1, setting.getSettingValue());
                ps.setInt(2, updatedBy);
                ps.setString(3, setting.getSettingKey());
                ps.addBatch();
            }
            
            int[] results = ps.executeBatch();
            
            for (int result : results) {
                if (result <= 0) return false;
            }
            
            return true;
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update multiple settings", e);
        }
    }

    @Override
    public boolean create(Setting setting) {
        String sql = "INSERT INTO settings (setting_key, setting_value, setting_category, description, updated_by) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, setting.getSettingKey());
            ps.setString(2, setting.getSettingValue());
            ps.setString(3, setting.getSettingCategory());
            ps.setString(4, setting.getDescription());
            ps.setObject(5, setting.getUpdatedBy());
            
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create setting", e);
        }
    }

    private Setting mapRow(ResultSet rs) throws SQLException {
        Setting setting = new Setting();
        setting.setSettingId(rs.getInt("setting_id"));
        setting.setSettingKey(rs.getString("setting_key"));
        setting.setSettingValue(rs.getString("setting_value"));
        setting.setSettingCategory(rs.getString("setting_category"));
        setting.setDescription(rs.getString("description"));
        setting.setUpdatedAt(rs.getTimestamp("updated_at"));
        
        int updatedBy = rs.getInt("updated_by");
        setting.setUpdatedBy(rs.wasNull() ? null : updatedBy);
        
        return setting;
    }
}