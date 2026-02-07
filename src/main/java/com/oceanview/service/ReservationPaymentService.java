// ReservationPaymentService.java
package com.oceanview.service;

import com.oceanview.dao.ReservationDAO;
import com.oceanview.dao.ReservationPaymentDAO;
import com.oceanview.dao.impl.ReservationDAOImpl;
import com.oceanview.dao.impl.ReservationPaymentDAOImpl;
import com.oceanview.model.Reservation;
import com.oceanview.model.ReservationPayment;

import java.sql.Timestamp;
import java.util.List;

public class ReservationPaymentService {

    private final ReservationPaymentDAO paymentDao = new ReservationPaymentDAOImpl();
    private final ReservationDAO reservationDao = new ReservationDAOImpl();

    public int addPayment(int reservationId, double amount, String method, String note, int createdBy) {

        if (reservationId <= 0) throw new IllegalArgumentException("Invalid reservation");
        if (amount <= 0) throw new IllegalArgumentException("Payment amount must be greater than zero");
        if (createdBy <= 0) throw new IllegalArgumentException("Session expired. Please login again.");

        Reservation reservation = reservationDao.findById(reservationId);
        if (reservation == null) throw new IllegalArgumentException("Reservation not found");

        double totalAmount = reservation.getTotalAmount();
        double currentPaid = reservation.getAmountPaid();
        double remaining = totalAmount - currentPaid;

        if (remaining <= 0) {
            throw new IllegalArgumentException("Reservation already fully paid");
        }

        if (amount > remaining) {
            throw new IllegalArgumentException("Payment amount exceeds remaining balance");
        }

        ReservationPayment payment = new ReservationPayment();
        payment.setReservationId(reservationId);
        payment.setPaidAmount(round2(amount));
        payment.setPaidDate(new Timestamp(System.currentTimeMillis()));
        payment.setMethod(method == null ? "" : method.trim());
        payment.setNote(note == null ? "" : note.trim());
        payment.setCreatedBy(createdBy);

        int paymentId = paymentDao.create(payment);
        if (paymentId <= 0) throw new IllegalStateException("Payment insert failed");

        double newPaidAmount = round2(currentPaid + amount);
        String paymentStatus = calculatePaymentStatus(newPaidAmount, totalAmount);

        reservationDao.updatePaymentStatus(reservationId, newPaidAmount, paymentStatus);

        return paymentId;
    }

    public boolean deletePayment(int paymentId) {

        if (paymentId <= 0) throw new IllegalArgumentException("Invalid payment id");

        ReservationPayment targetPayment = paymentDao.findById(paymentId);
        if (targetPayment == null) return false;

        int reservationId = targetPayment.getReservationId();
        double deletedAmount = targetPayment.getPaidAmount();

        boolean deleted = paymentDao.delete(paymentId);
        if (!deleted) return false;

        Reservation reservation = reservationDao.findById(reservationId);
        if (reservation != null) {
            double newPaidAmount = round2(reservation.getAmountPaid() - deletedAmount);
            if (newPaidAmount < 0) newPaidAmount = 0;

            String paymentStatus = calculatePaymentStatus(newPaidAmount, reservation.getTotalAmount());
            reservationDao.updatePaymentStatus(reservationId, newPaidAmount, paymentStatus);
        }

        return true;
    }

    public List<ReservationPayment> getPaymentHistory(int reservationId) {

        if (reservationId <= 0) throw new IllegalArgumentException("Invalid reservation id");
        return paymentDao.findByReservation(reservationId);
    }

    private String calculatePaymentStatus(double amountPaid, double totalAmount) {

        if (amountPaid <= 0) return "UNPAID";
        if (amountPaid >= totalAmount) return "PAID";
        return "PARTIAL";
    }

    private double round2(double v) {

        return Math.round(v * 100.0) / 100.0;
    }
}
