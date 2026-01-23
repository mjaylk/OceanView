package com.oceanview.model;

public class Room {
    private int roomId;
    private String roomNumber;
    private String roomType;
    private double ratePerNight;
    private String status;
    private int maxGuests;
   
    public Room() {}
    
    public Room(String roomNumber, String roomType, double ratePerNight, String status, int maxGuests) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.ratePerNight = ratePerNight;
        this.status = status;
        this.maxGuests = maxGuests;
    }
    
    // Getters/Setters

    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public double getRatePerNight() { return ratePerNight; }
    public void setRatePerNight(double ratePerNight) { this.ratePerNight = ratePerNight; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public int getMaxGuests() {return maxGuests;}
    public void setMaxGuests(int maxGuests) {this.maxGuests = maxGuests;}

    @Override
    public String toString() {
        return String.format("Room{id=%d, number='%s', type='%s', price=%.2f, status='%s'}", 
                           roomId, roomNumber, roomType, ratePerNight, status);
    }
}
