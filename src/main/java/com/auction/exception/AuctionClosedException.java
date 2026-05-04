package com.auction.exception;

public class AuctionClosedException extends RuntimeException {
    public AuctionClosedException(String message) {
        super(message);
    }
}
// ngăn chặn các hành động tác động vào một cuộc đấu giá đã kết thúc