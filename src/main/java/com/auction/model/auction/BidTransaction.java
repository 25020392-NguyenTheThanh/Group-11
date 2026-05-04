package com.auction.model.auction;


import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

// Ghi lại một lần đặt giá trong phiên

public class BidTransaction {
    private final int bidderId;
    private final String bidderName;
    private final double amount;
    private final LocalDateTime timestamp;

    public BidTransaction(int bidderId, String bidderName, double amount) {
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public int getBidderId() {
        return bidderId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    @Override
    public String toString() {
        return String.format("  [%s] %s đặt %.0f₫",
                timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")), bidderName, amount);
    }
}
