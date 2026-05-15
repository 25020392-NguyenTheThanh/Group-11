package com.auction.network;

import java.io.Serializable;
import java.time.LocalDateTime;

public class CreateAuctionPayload implements Serializable {
    public int itemId ;
    public LocalDateTime endTime ;
    public double minBidStep ;
}
