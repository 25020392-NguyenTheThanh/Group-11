package com.auction.model.auction;

import java.io.Serializable;

/**
 * Cấu hình auto-bid cho 1 bidder trong 1 phiên đấu giá.
 * maxBid  = mức giá tối đa bidder chịu bỏ ra
 * increment = mỗi lần auto-bid tăng thêm bao nhiêu (tối thiểu bằng minBidStep)
 */

public class AutoBidConfig implements Serializable {
    private static final long serialVersionUID = 1L ;
    private final int bidderId ;
    private final String bidderUsername ;
    private final double maxBid ;
    private final double increment ;
    public AutoBidConfig(int bidderId , String bidderUserName , double maxBid , double increment){
        this.bidderId = bidderId ;
        this.bidderUsername = bidderUserName ;
        this.maxBid = maxBid ;
        this.increment =increment ;
    }
    public int getBidderId()         { return bidderId; }
    public String getBidderUsername(){ return bidderUsername; }
    public double getMaxBid()        { return maxBid; }
    public double getIncrement()     { return increment; }
}
