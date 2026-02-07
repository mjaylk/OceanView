package com.oceanview.model;

import java.sql.Date;

public class ReservationDailyCount {

    // model class
    // report data

    private Date day;
    private int count;

    // default constructor
    public ReservationDailyCount() {
    }

    // parameter constructor
    public ReservationDailyCount(Date day, int count) {
        this.day = day;
        this.count = count;
    }

    // getter setter
    public Date getDay() {
        return day;
    }

    public void setDay(Date day) {
        this.day = day;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
