package com.auction.exception;

public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
// Xử lý các lỗi liên quan đến xác thực người dùng