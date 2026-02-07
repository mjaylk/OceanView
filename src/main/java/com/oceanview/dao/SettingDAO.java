package com.oceanview.dao;

import com.oceanview.model.Setting;
import java.util.List;

public interface SettingDAO {

    
    // abstraction

    List<Setting> findAll();                    // read all
    List<Setting> findByCategory(String category); // grouping
    Setting findByKey(String key);              // config lookup

    String getValue(String key);                // config read
    String getValue(String key, String defaultValue); // fallback logic

    boolean update(String key, String value, int updatedBy); // update config
    boolean updateMultiple(List<Setting> settings, int updatedBy); // bulk update
    boolean create(Setting setting);            // create
}
