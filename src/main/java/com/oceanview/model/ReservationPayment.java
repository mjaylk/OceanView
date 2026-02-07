package com.oceanview.model;

import java.sql.Timestamp;

public class ReservationPayment {

    // model class
    // payment record holder

    private int paymentId;
    private int reservationId;
    private double paidAmount;
    private Timestamp paidDate;
    private String method;
    private String note;
    private int createdBy;

    // default constructor
    public ReservationPayment() {}

    // getter setter
    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }

    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }

    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }

    public Timestamp getPaidDate() { return paidDate; }
    public void setPaidDate(Timestamp paidDate) { this.paidDate = paidDate; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }
}