package com.auction.exception;

public class InvalidBidException extends RuntimeException {
    public InvalidBidException(String message) {
        super(message);
    }
}
// xử lý các trường hợp đặt giá thầu không hợp lệ