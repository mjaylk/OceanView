package com.oceanview.service;

import com.oceanview.model.Reservation;
import org.junit.jupiter.api.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationServiceTest {

    private ReservationService service;

    @BeforeEach
    void setup() {
        service = new ReservationService();
    }

    @Test
    void TEST_CASE_01_getByNumber_shouldReturnNull_whenInputNull() {
        Reservation r = service.getByNumber(null);
        assertNull(r);
    }

    @Test
    void TEST_CASE_02_getBetween_shouldThrowException_whenDatesNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.getBetween(null, null);
        });
    }

    @Test
    void TEST_CASE_03_listReservations_shouldReturnList_notNull() {
        List<Reservation> list = service.listReservations();
        assertNotNull(list);
    }

    @Test
    void TEST_CASE_04_listRoomsWithAvailabilityJson_shouldThrowException_whenInvalidRange() {
        Date today = Date.valueOf(LocalDate.now());

        assertThrows(IllegalArgumentException.class, () -> {
            service.listRoomsWithAvailabilityJson(today, today);
        });
    }

    @Test
    void TEST_CASE_05_deleteReservation_shouldThrowException_whenInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.deleteReservation(0);
        });
    }

    @Test
    void TEST_CASE_06_getDashboardStatsJson_shouldReturnValidJson() {
        String json = service.getDashboardStatsJson(7);

        assertNotNull(json);
        assertTrue(json.contains("\"success\":true"));
    }
}
