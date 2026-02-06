package com.oceanview.model;

import java.sql.Date;

public class ReservationDailyCount {

    private Date day;
    private int count;

    public ReservationDailyCount() {
    }

    public ReservationDailyCount(Date day, int count) {
        this.day = day;
        this.count = count;
    }

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
