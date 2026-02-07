package com.oceanview.web.dto;

public class UserRequest {

    // user id
    public int userId;

    // username
    public String username;

    // password
    public String password;

    // user role
    public String role;

    // user status
    public String status;

    // simple manual test
    public static void main(String[] args) {

        System.out.println("TEST CASE 01 - Create UserRequest object");

        UserRequest req = new UserRequest();
        req.userId = 1;
        req.username = "testuser";
        req.password = "test123";
        req.role = "ADMIN";
        req.status = "ACTIVE";

        if (req.userId == 1 && "testuser".equals(req.username)) {
            System.out.println("RESULT: PASS");
        } else {
            System.out.println("RESULT: FAIL");
        }
    }
}
