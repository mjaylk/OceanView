package com.oceanview.service;

import com.oceanview.dao.GuestDAO;
import com.oceanview.dao.impl.GuestDAOImpl;
import com.oceanview.model.Guest;

import java.util.List;

public class GuestService {

    private final GuestDAO dao = new GuestDAOImpl();

    public List<Guest> listGuests() { return dao.findAll(); }


    public Guest getGuestById(int id) {
        if (id <= 0) return null;
        return dao.findById(id);
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

    public int createGuest(Integer userId, String fullName, String address,
                           String contactNumber, String email, String password) {

        if (fullName == null || fullName.trim().isEmpty())
            throw new IllegalArgumentException("Full name required");
        if (contactNumber == null || contactNumber.trim().isEmpty())
            throw new IllegalArgumentException("Contact number required");

        Guest g = new Guest();
        g.setGuestId(0);
        g.setUserId(userId);
        g.setFullName(fullName.trim());
        g.setAddress(address != null && !address.trim().isEmpty() ? address.trim() : null);
        g.setContactNumber(contactNumber.trim());
        g.setEmail(email != null && !email.trim().isEmpty() ? email.trim() : null);

        String pw = (password == null || password.trim().isEmpty()) ? "123456" : password.trim();
        g.setPassword(pw);

        return dao.create(g);
    }

 
    public boolean updateGuest(int guestId, String fullName, String address,
                               String contactNumber, String email) {

        if (guestId <= 0) throw new IllegalArgumentException("guestId required");
        if (fullName == null || fullName.trim().isEmpty())
            throw new IllegalArgumentException("Full name required");
        if (contactNumber == null || contactNumber.trim().isEmpty())
            throw new IllegalArgumentException("Contact number required");

        Guest existing = dao.findById(guestId);
        if (existing == null) return false;

        existing.setFullName(fullName.trim());
        existing.setAddress(address != null && !address.trim().isEmpty() ? address.trim() : null);
        existing.setContactNumber(contactNumber.trim());
        existing.setEmail(email != null && !email.trim().isEmpty() ? email.trim() : null);

        return dao.update(existing);
    }

    
    public int getGuestIdByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return 0;
        Guest g = dao.findByEmail(email.trim());
        return (g == null) ? 0 : g.getGuestId();
    }

    public void updateGuestPassword(int guestId, String password) {
        if (guestId <= 0) throw new IllegalArgumentException("Invalid guestId");
        if (password == null || password.trim().isEmpty()) throw new IllegalArgumentException("Password required");

        boolean ok = dao.updatePassword(guestId, password.trim());
        if (!ok) throw new RuntimeException("Failed to update guest password");
    }

   
    public boolean deleteGuest(int guestId) {
        if (guestId <= 0) throw new IllegalArgumentException("guestId required");
        return dao.delete(guestId);
    }


    public int ensureGuest(String fullName, String email, String contactNumber) {
        return ensureGuestWithPassword(fullName, email, contactNumber, null);
    }

    public int ensureGuestWithPassword(String fullName, String email, String contactNumber, String password) {

        if (fullName == null || fullName.trim().isEmpty())
            throw new IllegalArgumentException("Guest name required");

        if (contactNumber == null || contactNumber.trim().isEmpty())
            throw new IllegalArgumentException("Contact number required");

        String phone = contactNumber.trim();
        String em = (email == null ? null : email.trim());

        Guest g = dao.findByContactNumber(phone);
        if (g == null && em != null && !em.isEmpty()) {
            g = dao.findByEmail(em);
        }

        String pw = (password == null || password.trim().isEmpty()) ? "123456" : password.trim();

        if (g != null) {
            if (g.getPassword() == null || g.getPassword().trim().isEmpty()) {
                dao.updatePasswordById(g.getGuestId(), pw);
            }
            return g.getGuestId();
        }

        Guest ng = new Guest();
        ng.setUserId(null);
        ng.setFullName(fullName.trim());
        ng.setAddress(null);
        ng.setContactNumber(phone);
        ng.setEmail((em != null && !em.isEmpty()) ? em : null);
        ng.setPassword(pw);

        int id = dao.create(ng);
        if (id <= 0) throw new IllegalStateException("Guest creation failed");
        return id;
    }

    public Guest loginGuest(String email, String password) {
        if (email == null || email.trim().isEmpty()) throw new IllegalArgumentException("Email required");
        if (password == null || password.trim().isEmpty()) throw new IllegalArgumentException("Password required");
        return dao.findByEmailAndPassword(email.trim(), password.trim());
    }
}
