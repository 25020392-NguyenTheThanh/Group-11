package com.auction.security;

import java.util.regex.Pattern;

/**
 * Kiểm tra và làm sạch dữ liệu đầu vào từ người dùng.
 * Dùng trước khi xử lý ở server để chặn injection và dữ liệu rác.
 */
public class InputValidator {

    // Username: chỉ cho phép chữ, số, dấu gạch dưới, 3-30 ký tự
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");

    // Email cơ bản
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    // Ký tự nguy hiểm cho SQL/script (phòng trường hợp dùng ngoài PreparedStatement)
    private static final Pattern DANGEROUS_CHARS = Pattern.compile("[<>\"'`;]");

    /** Kiểm tra username hợp lệ */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    /** Kiểm tra email hợp lệ */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Kiểm tra mật khẩu đủ mạnh:
     * - Tối thiểu 8 ký tự
     * - Có ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper  = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower  = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit  = password.chars().anyMatch(Character::isDigit);
        return hasUpper && hasLower && hasDigit;
    }

    /** Kiểm tra role hợp lệ */
    public static boolean isValidRole(String role) {
        return "BIDDER".equalsIgnoreCase(role) || "SELLER".equalsIgnoreCase(role);
    }

    /** Loại bỏ ký tự nguy hiểm khỏi chuỗi văn bản tự do (tên sản phẩm, mô tả...) */
    public static String sanitize(String input) {
        if (input == null) return "";
        return DANGEROUS_CHARS.matcher(input.trim()).replaceAll("");
    }

    /** Thông báo lỗi chi tiết khi password không đủ mạnh */
    public static String getPasswordError(String password) {
        if (password == null || password.isEmpty()) return "Mật khẩu không được để trống!";
        if (password.length() < 8)                  return "Mật khẩu phải có ít nhất 8 ký tự!";
        if (password.chars().noneMatch(Character::isUpperCase)) return "Mật khẩu phải có ít nhất 1 chữ hoa!";
        if (password.chars().noneMatch(Character::isLowerCase)) return "Mật khẩu phải có ít nhất 1 chữ thường!";
        if (password.chars().noneMatch(Character::isDigit))     return "Mật khẩu phải có ít nhất 1 chữ số!";
        return null; // hợp lệ
    }
}