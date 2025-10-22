package com.asbitech.client_ms.infra.utils;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;

public class TOTPUtils {

    private static final GoogleAuthenticator gAuth;

    static {
        // Configure GoogleAuthenticator with custom windowSize
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setTimeStepSizeInMillis(60000) // 30 seconds per code (standard)
                .setWindowSize(3)                // Accept ±1 window (friendly for users)
                .build();
        gAuth = new GoogleAuthenticator(config);
    }

    private TOTPUtils() {
        throw new UnsupportedOperationException("TOTP class");
    }

    public static String generateSecretKey() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey(); // This is the secret key the user must save
    }

    public static String getOtpAuthURL(String accountName, String secretKey) {
        String issuer = "ClientPortal"; // TODO: move to application.properties
        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                issuer, accountName, new GoogleAuthenticatorKey.Builder(secretKey).build());
    }

    public static int getOTP(String secretKey) {
        return gAuth.getTotpPassword(secretKey);
    }

    public static boolean validateOTP(String secretKey, int otp) {
        return gAuth.authorize(secretKey, otp);
    }
}
