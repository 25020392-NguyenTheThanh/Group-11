package com.auction.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Tiện ích băm mật khẩu dùng SHA-256 + Salt ngẫu nhiên.
 *
 * Định dạng lưu trong DB: "<salt_base64>:<hash_base64>"
 * VD: "aBc123...==:XyZ456...=="
 *
 * Không dùng thư viện ngoài — chỉ dùng java.security có sẵn trong JDK.
 */
public class PasswordUtil {

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_BYTES   = 16; // 128-bit salt

    // Không cho phép khởi tạo
    private PasswordUtil() {}

    /**
     * Băm mật khẩu với salt ngẫu nhiên.
     * @return chuỗi "<salt_base64>:<hash_base64>" để lưu vào DB
     */
    public static String hash(String plainPassword) {
        byte[] salt = generateSalt();
        byte[] hashed = sha256(plainPassword, salt);
        return Base64.getEncoder().encodeToString(salt)
                + ":"
                + Base64.getEncoder().encodeToString(hashed);
    }

    /**
     * Xác minh mật khẩu người dùng nhập so với chuỗi đã lưu trong DB.
     * @param plainPassword  mật khẩu người dùng nhập vào
     * @param storedValue    giá trị "<salt>:<hash>" lưu trong DB
     * @return true nếu khớp
     */
    public static boolean verify(String plainPassword, String storedValue) {
        if (plainPassword == null || storedValue == null) return false;
        String[] parts = storedValue.split(":", 2);
        if (parts.length != 2) {
            // Trường hợp DB cũ vẫn lưu plain text (chưa migrate) — so sánh trực tiếp
            return plainPassword.equals(storedValue);
        }
        byte[] salt   = Base64.getDecoder().decode(parts[0]);
        byte[] stored = Base64.getDecoder().decode(parts[1]);
        byte[] attempt = sha256(plainPassword, salt);
        return constantTimeEquals(stored, attempt);
    }

    /**
     * Kiểm tra xem chuỗi đã được hash chưa (có format "<salt>:<hash>" không).
     * Dùng để migrate dữ liệu cũ.
     */
    public static boolean isHashed(String value) {
        if (value == null) return false;
        String[] parts = value.split(":", 2);
        if (parts.length != 2) return false;
        try {
            Base64.getDecoder().decode(parts[0]);
            Base64.getDecoder().decode(parts[1]);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    //  Private helpers

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return salt;
    }

    private static byte[] sha256(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);
            return md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 luôn có sẵn trong JDK — không bao giờ xảy ra
            throw new RuntimeException("SHA-256 không khả dụng", e);
        }
    }

    /**
     * So sánh 2 mảng byte trong thời gian cố định (chống timing attack).
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= (a[i] ^ b[i]);
        }
        return diff == 0;
    }
}
