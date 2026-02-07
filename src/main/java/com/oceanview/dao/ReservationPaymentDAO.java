// ReservationPaymentDAO.java
package com.oceanview.dao;

import com.oceanview.model.ReservationPayment;

import java.util.List;

public interface ReservationPaymentDAO {

    int create(ReservationPayment payment);
    List<ReservationPayment> findByReservation(int reservationId);
    double sumPaymentsByReservation(int reservationId);
    boolean delete(int paymentId);

    ReservationPayment findById(int paymentId);
}
