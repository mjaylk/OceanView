package com.oceanview.model;

import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationTest {

    @Test
    void TEST_CASE_01_settersAndGetters_shouldReturnCorrectValues() {
        Reservation r = new Reservation();

        Date checkIn = Date.valueOf("2026-02-20");
        Date checkOut = Date.valueOf("2026-02-25");

        r.setReservationId(101);
        r.setReservationNumber("RES-001");
        r.setGuestId(10);
        r.setRoomId(5);
        r.setCheckInDate(checkIn);
        r.setCheckOutDate(checkOut);
        r.setStatus("CONFIRMED");
        r.setCreatedBy(1);

        r.setNights(5);
        r.setRatePerNight(15000);
        r.setSubtotal(75000);
        r.setTax(7500);
        r.setDiscount(5000);
        r.setTotalAmount(77500);

        r.setAmountPaid(30000);
        r.setPaymentStatus("PARTIAL");

        r.setGuestName("John Doe");
        r.setGuestEmail("john@email.com");
        r.setRoomNumber("A-101");
        r.setGuestContactNumber("0771234567");
        r.setRoomType("DELUXE");
        r.setNotes("Late arrival");

        assertEquals(101, r.getReservationId());
        assertEquals("RES-001", r.getReservationNumber());
        assertEquals(10, r.getGuestId());
        assertEquals(5, r.getRoomId());
        assertEquals(checkIn, r.getCheckInDate());
        assertEquals(checkOut, r.getCheckOutDate());
        assertEquals("CONFIRMED", r.getStatus());
        assertEquals(1, r.getCreatedBy());

        assertEquals(5, r.getNights());
        assertEquals(15000, r.getRatePerNight());
        assertEquals(75000, r.getSubtotal());
        assertEquals(7500, r.getTax());
        assertEquals(5000, r.getDiscount());
        assertEquals(77500, r.getTotalAmount());

        assertEquals(30000, r.getAmountPaid());
        assertEquals("PARTIAL", r.getPaymentStatus());

        assertEquals("John Doe", r.getGuestName());
        assertEquals("john@email.com", r.getGuestEmail());
        assertEquals("A-101", r.getRoomNumber());
        assertEquals("0771234567", r.getGuestContactNumber());
        assertEquals("DELUXE", r.getRoomType());
        assertEquals("Late arrival", r.getNotes());
    }

    @Test
    void TEST_CASE_02_defaultValues_shouldBeSafe() {
        Reservation r = new Reservation();

        assertEquals(0, r.getReservationId());
        assertNull(r.getReservationNumber());
        assertEquals(0, r.getGuestId());
        assertEquals(0, r.getRoomId());
        assertNull(r.getCheckInDate());
        assertNull(r.getCheckOutDate());
        assertNull(r.getStatus());

        assertEquals(0, r.getNights());
        assertEquals(0.0, r.getRatePerNight());
        assertEquals(0.0, r.getSubtotal());
        assertEquals(0.0, r.getTax());
        assertEquals(0.0, r.getDiscount());
        assertEquals(0.0, r.getTotalAmount());

        assertEquals(0.0, r.getAmountPaid());
        assertNull(r.getPaymentStatus());
    }

    @Test
    void TEST_CASE_03_updateValues_shouldOverwriteCorrectly() {
        Reservation r = new Reservation();

        r.setStatus("PENDING");
        assertEquals("PENDING", r.getStatus());

        r.setStatus("CONFIRMED");
        assertEquals("CONFIRMED", r.getStatus());
    }
}
