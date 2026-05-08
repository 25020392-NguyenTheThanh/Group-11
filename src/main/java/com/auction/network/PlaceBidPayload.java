package com.auction.network;


import java.io.Serializable;

public class  PlaceBidPayload implements Serializable {
    private static final long serialVersionUID = 1L;
    public int auctionId;
    public double amount;
}
