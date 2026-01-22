package com.oceanview.util;

/**
 * Password utility using BCrypt.
 *
 * Why BCrypt?
 * - Automatic salt
 * - Resistant to brute-force
 * - Industry standard
 */
public final class PasswordUtil {

    private PasswordUtil() {}

    public static String hashPassword(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }

    public static boolean verifyPassword(String plain, String hashed) {
        if (plain == null || hashed == null) return false;
        return BCrypt.checkpw(plain, hashed);
    }
}
