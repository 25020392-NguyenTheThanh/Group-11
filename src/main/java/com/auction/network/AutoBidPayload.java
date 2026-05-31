package com.auction.network;

import java.io.Serializable;

public class AutoBidPayload implements Serializable {
    private static final long serialVersionUID = 1L;
    public int auctionId;
    public double maxBid;
    public double increment;

    public AutoBidPayload(int auctionId, double maxBid, double increment) {
        this.auctionId = auctionId;
        this.maxBid = maxBid;
        this.increment = increment;
    }
}