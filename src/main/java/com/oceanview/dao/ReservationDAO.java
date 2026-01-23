package com.oceanview.dao;

import com.oceanview.model.Reservation;

import java.sql.Date;
import java.util.List;

public interface ReservationDAO {
    List<Reservation> findAll();
    Reservation findById(int id);
    Reservation findByNumber(String reservationNumber);

    boolean hasOverlappingReservation(int roomId, Date checkIn, Date checkOut);

    String findLastReservationNumberForDate(String yyyymmddPrefix);

    int create(Reservation reservation);
    boolean updateStatus(int reservationId, String status);
    boolean delete(int reservationId);

    // NEW for your UI
    List<Reservation> findByRoom(int roomId);
    List<Reservation> findBetween(Date start, Date end);
}
