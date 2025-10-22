package com.asbitech.client_ms.infra.utils;

import java.security.SecureRandom;

public class PasswordUtils {
    // Private constructor to prevent instantiation
    private PasswordUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARACTERS = ".";
    
    private static final String ALL_CHARACTERS = LOWERCASE + UPPERCASE + DIGITS + SPECIAL_CHARACTERS;

    private static final SecureRandom random = new SecureRandom();

    public static String generateTempPassword(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("Password length must be at least 8 characters.");
        }

        StringBuilder password = new StringBuilder(length);

        // Ensure the password has at least one lowercase, one uppercase, one digit, and one special character
        password.append(getRandomChar(LOWERCASE));
        password.append(getRandomChar(UPPERCASE));
        password.append(getRandomChar(DIGITS));
        password.append(getRandomChar(SPECIAL_CHARACTERS));

        // Fill the rest of the password with random characters from the allowed characters
        for (int i = 4; i < length; i++) {
            password.append(getRandomChar(ALL_CHARACTERS));
        }

        // Shuffle the characters to avoid predictable patterns
        return shuffleString(password.toString());
    }

    private static char getRandomChar(String charPool) {
        int index = random.nextInt(charPool.length());
        return charPool.charAt(index);
    }

    private static String shuffleString(String input) {
        char[] array = input.toCharArray();
        for (int i = 0; i < array.length; i++) {
            int j = random.nextInt(array.length);
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
        
        return new String(array);
    }
}
