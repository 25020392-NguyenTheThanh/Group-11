package com.auction.model.auction;

import com.auction.model.user.Bidder;

import java.time.LocalDateTime;

public class BidTransaction {
    private Bidder bidder ;
    private double amount ;
    private LocalDateTime timestamp ; // thời điểm đặt giá

    public BidTransaction(Bidder bidder , double amount ){
        this.bidder = bidder ;
        this.amount = amount ;
        this.timestamp = LocalDateTime.now() ;
    }

    public Bidder getBidder() { return bidder; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
