package com.oceanview.service;

import com.oceanview.dao.GuestDAO;
import com.oceanview.dao.impl.GuestDAOImpl;
import com.oceanview.model.Guest;

import java.util.List;

public class GuestService {

    private final GuestDAO dao = new GuestDAOImpl();

    public List<Guest> listGuests() {
        return dao.findAll();
    }

    public Guest getGuestByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return null;
        return dao.findByEmail(email.trim());
    }

    public Guest getGuestByContactNumber(String contact) {
        if (contact == null || contact.trim().isEmpty()) return null;
        return dao.findByContactNumber(contact.trim());
    }

    public List<Guest> searchGuests(String q) {
        if (q == null || q.trim().length() < 2) return List.of();
        return dao.search(q.trim(), 10);
    }
 public int createGuest(Integer userId, String fullName, String address, String contactNumber, String email) {
    if (fullName == null || fullName.trim().isEmpty())
        throw new IllegalArgumentException("Full name required");
    if (contactNumber == null || contactNumber.trim().isEmpty())
        throw new IllegalArgumentException("Contact number required");

    Guest g = new Guest();
    g.setGuestId(0); // let DAO assign
    g.setUserId(userId);
    g.setFullName(fullName.trim());
    g.setAddress(address != null && !address.trim().isEmpty() ? address.trim() : null);
    g.setContactNumber(contactNumber.trim());
    g.setEmail(email != null && !email.trim().isEmpty() ? email.trim() : null);

    return dao.create(g);
}


    public int ensureGuest(String fullName, String email, String contactNumber) {

        if (fullName == null || fullName.trim().isEmpty())
            throw new IllegalArgumentException("Guest name required");

        if (contactNumber == null || contactNumber.trim().isEmpty())
            throw new IllegalArgumentException("Contact number required");

        Guest g = dao.findByContactNumber(contactNumber.trim());
        if (g != null) return g.getGuestId();

        // 2️⃣ fallback to email
        if (email != null && !email.trim().isEmpty()) {
            g = dao.findByEmail(email.trim());
            if (g != null) return g.getGuestId();
        }

        Guest ng = new Guest();
        ng.setUserId(null);
        ng.setFullName(fullName.trim());
        ng.setAddress(null);
        ng.setContactNumber(contactNumber.trim());
        ng.setEmail(email != null ? email.trim() : null);

        int id = dao.create(ng);
        if (id <= 0) throw new IllegalStateException("Guest creation failed");
        return id;
    }
}
