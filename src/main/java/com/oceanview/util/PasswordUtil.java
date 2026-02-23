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


}
