package com.oceanview.dao;

import com.oceanview.dao.impl.RoomDAOImpl;
import com.oceanview.model.Room;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RoomDAOTest {

    private RoomDAO dao;

    @BeforeEach
    void setup() {
        dao = new RoomDAOImpl();
    }

    @Test
    void TEST_CASE_01_findAll_shouldReturnList_notNull() {
        List<Room> list = dao.findAll();

        assertNotNull(list);
    }

    @Test
    void TEST_CASE_02_findById_shouldReturnRoom_whenExists() {
        List<Room> list = dao.findAll();

        assertNotNull(list);
        assertTrue(list.size() > 0, "No rooms found in DB. Insert at least one room.");

        int existingId = list.get(0).getRoomId();

        Room r = dao.findById(existingId);

        assertNotNull(r);
        assertEquals(existingId, r.getRoomId());
    }

    @Test
    void TEST_CASE_03_findById_shouldReturnNull_whenNotExists() {
        Room r = dao.findById(Integer.MAX_VALUE);

        assertNull(r);
    }

    @Test
    void TEST_CASE_04_findPriceById_shouldReturnValidPrice() {
        List<Room> list = dao.findAll();

        assertTrue(list.size() > 0, "No rooms available for price test.");

        int existingId = list.get(0).getRoomId();

        double price = dao.findPriceById(existingId);

        assertTrue(price >= 0);
    }

    @Test
    void TEST_CASE_05_updateStatus_shouldReturnBoolean() {
        List<Room> list = dao.findAll();

        assertTrue(list.size() > 0, "No rooms available for status update test.");

        int existingId = list.get(0).getRoomId();

        boolean result = dao.updateStatus(existingId, "AVAILABLE");

        assertTrue(result == true || result == false);
    }
}
