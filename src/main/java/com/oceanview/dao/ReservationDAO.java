package com.oceanview.dao;

import com.oceanview.model.Reservation;
import com.oceanview.model.ReservationDailyCount;


import java.sql.Date;
import java.util.List;

public interface ReservationDAO {
    List<Reservation> findAll();
    Reservation findById(int id);
    Reservation findByNumber(String reservationNumber);
    int countAllReservations();
    boolean hasBookingInRange(int roomId, Date checkIn, Date checkOut);

    int countOccupiedRoomsToday(Date today);


    boolean hasOverlappingReservation(int roomId, Date checkIn, Date checkOut);

   
    boolean hasOverlappingReservationExceptSelf(int roomId, int reservationId, Date checkIn, Date checkOut);

    String findLastReservationNumberForDate(String yyyymmddPrefix);

    int create(Reservation reservation);
    boolean updateStatus(int reservationId, String status);
    boolean delete(int reservationId);

    boolean update(Reservation r);

    List<Reservation> findByRoom(int roomId);
    List<Reservation> findBetween(Date start, Date end);
    List<Reservation> getRecentCheckins();
    List<Reservation> findByGuestId(int guestId);
    
    int countBetween(Date start, Date end);

    double sumRevenueBetween(Date start, Date end);

    List<ReservationDailyCount> countPerDayBetween(Date start, Date end);



}
