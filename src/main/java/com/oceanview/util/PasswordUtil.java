package com.oceanview.util;

public final class PasswordUtil {

    // utility class
    private PasswordUtil() {}

    // hashing method
    public static String hashPassword(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }

    // verification method
    public static boolean verifyPassword(String plain, String hashed) {
        if (plain == null || hashed == null) return false;
        return BCrypt.checkpw(plain, hashed);
    }

    // test cases
    public static void main(String[] args) {

        // test case 01 hash password
        String password = "test123";
        String hash = hashPassword(password);
        System.out.println("Test 01 Hash created: " + (hash != null));

        // test case 02 correct password
        boolean result1 = verifyPassword("test123", hash);
        System.out.println("Test 02 Correct password: " + result1);

        // test case 03 wrong password
        boolean result2 = verifyPassword("wrong123", hash);
        System.out.println("Test 03 Wrong password: " + (!result2));

        // test case 04 null input
        boolean result3 = verifyPassword(null, hash);
        System.out.println("Test 04 Null password: " + (!result3));
    }
}
