package com.oceanview.dao;

import com.oceanview.model.Setting;
import java.util.List;

public interface SettingDAO {
    List<Setting> findAll();
    List<Setting> findByCategory(String category);
    Setting findByKey(String key);
    String getValue(String key);
    String getValue(String key, String defaultValue);
    boolean update(String key, String value, int updatedBy);
    boolean updateMultiple(List<Setting> settings, int updatedBy);
    boolean create(Setting setting);
    
}