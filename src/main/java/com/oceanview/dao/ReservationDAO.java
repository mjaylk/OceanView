package com.oceanview.dao;

import com.oceanview.model.Reservation;
import com.oceanview.model.ReservationDailyCount;

import java.sql.Date;
import java.util.List;

public interface ReservationDAO {

  
    // abstraction

    List<Reservation> findAll();                    // read all
    Reservation findById(int id);                  // read by id
    Reservation findByNumber(String reservationNumber); // business key

    int countAllReservations();                    // reporting
    boolean hasBookingInRange(int roomId, Date checkIn, Date checkOut); // validation

    int countOccupiedRoomsToday(Date today);       // dashboard stats

    boolean hasOverlappingReservation(int roomId, Date checkIn, Date checkOut); // conflict check
    boolean hasOverlappingReservationExceptSelf(
            int roomId, int reservationId, Date checkIn, Date checkOut); // update validation

    String findLastReservationNumberForDate(String yyyymmddPrefix); // number generation

    int create(Reservation reservation);            // create
    boolean updateStatus(int reservationId, String status); // status change
    boolean delete(int reservationId);              // delete
    boolean update(Reservation r);                  // update

    List<Reservation> findByRoom(int roomId);       // filter by room
    List<Reservation> findBetween(Date start, Date end); // date range
    List<Reservation> getRecentCheckins();          // dashboard
    List<Reservation> findByGuestId(int guestId);   // guest history

    int countBetween(Date start, Date end);         // analytics
    double sumRevenueBetween(Date start, Date end); // revenue

    List<ReservationDailyCount> countPerDayBetween(
            Date start, Date end);                  // chart data
 // payment support methods
    boolean updatePaymentStatus(int reservationId, double amountPaid, String paymentStatus);
}
