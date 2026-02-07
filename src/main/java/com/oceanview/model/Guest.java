package com.oceanview.model;

public class Guest {

    // model class
    // data holder

    private int guestId;
    private Integer userId;        
    private String fullName;
    private String address;
    private String contactNumber;
    private String email;
    private String password;

    // default constructor
    public Guest() {}

    // getter setter
    public int getGuestId() { return guestId; }
    public void setGuestId(int guestId) { this.guestId = guestId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // sensitive data
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
