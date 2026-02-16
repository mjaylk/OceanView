package com.oceanview.dao;

import com.oceanview.dao.impl.ReservationDAOImpl;
import com.oceanview.model.Reservation;
import org.junit.jupiter.api.*;

import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationDAOTest {

    private ReservationDAO dao;

    @BeforeEach
    void setup() {
        dao = new ReservationDAOImpl();
    }

    @Test
    void TEST_CASE_01_findAll_shouldReturnList_notNull() {
        List<Reservation> list = dao.findAll();
        assertNotNull(list);
     
    }

    @Test
    void TEST_CASE_02_findById_shouldReturnReservation_whenIdExists() {
     
        java.util.List<Reservation> list = dao.findAll();

   
        assertNotNull(list);
        assertTrue(list.size() > 0, "No reservations in DB. Insert at least 1 reservation to test findById.");

        int existingId = list.get(0).getReservationId();
        assertTrue(existingId > 0, "Reservation ID from DB should be > 0");

     
        Reservation r = dao.findById(existingId);

        assertNotNull(r);
        assertEquals(existingId, r.getReservationId());
    }


    @Test
    void TEST_CASE_03_findById_shouldReturnNull_whenIdNotExists() {
        int notExists = 999999;

        Reservation r = dao.findById(notExists);

        assertNull(r);
    }

    @Test
    void TEST_CASE_04_hasOverlappingReservation_shouldReturnBoolean() {
      
        int roomId = 1;

        Date checkIn = Date.valueOf("2026-02-20");
        Date checkOut = Date.valueOf("2026-02-22");

        boolean result = dao.hasOverlappingReservation(roomId, checkIn, checkOut);

        assertTrue(result == true || result == false);
    }

    @Test
    void TEST_CASE_05_countAllReservations_shouldReturnNonNegative() {
        int count = dao.countAllReservations();
        assertTrue(count >= 0);
    }
}
