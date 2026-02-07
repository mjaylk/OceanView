package com.oceanview.model;

import java.sql.Timestamp;

public class Setting {

    // model class
    // config data

    private int settingId;
    private String settingKey;
    private String settingValue;
    private String settingCategory;
    private String description;
    private Timestamp updatedAt;
    private Integer updatedBy;

    // default constructor
    public Setting() {}

    // parameter constructor
    public Setting(String settingKey, String settingValue, String settingCategory) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.settingCategory = settingCategory;
    }

    // getter setter
    public int getSettingId() {
        return settingId;
    }

    public void setSettingId(int settingId) {
        this.settingId = settingId;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public String getSettingCategory() {
        return settingCategory;
    }

    public void setSettingCategory(String settingCategory) {
        this.settingCategory = settingCategory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

  
    public Integer getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Integer updatedBy) {
        this.updatedBy = updatedBy;
    }
}
