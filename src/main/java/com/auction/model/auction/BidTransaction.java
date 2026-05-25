package com.auction.model.auction;


import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

// Ghi lại một lần đặt giá trong phiên

public class BidTransaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int bidderId;
    private final String bidderName;
    private final double amount;
    private final LocalDateTime timestamp;
    private final String bidType; // "MANUAL" or "AUTO"

    public BidTransaction(int bidderId, String bidderName, double amount) {
        this(bidderId, bidderName, amount, "MANUAL");
    }

    public BidTransaction(int bidderId, String bidderName, double amount, String bidType) {
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.bidType = bidType != null ? bidType : "MANUAL";
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

    public String getBidderUsername() { return bidderName ; }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getBidType() {
        return bidType;
    }

    @Override
    public String toString() {
        return String.format("  [%s][%s] %s đặt %.0f₫",
                timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")), bidType, bidderName, amount);
    }
}
