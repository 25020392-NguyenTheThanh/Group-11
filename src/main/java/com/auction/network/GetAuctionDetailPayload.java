package com.auction.network;

import java.io.Serializable;

/**
 * Payload chứa thông tin yêu cầu chi tiết phiên đấu giá.
 */
public class GetAuctionDetailPayload implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // ID của phiên đấu giá
    public int auctionId;
    
    // Cờ đánh dấu có tăng lượt xem (viewCount) hay không (true khi click CHI TIẾT, false khi refresh realtime)
    public boolean incrementView;

    public GetAuctionDetailPayload(int auctionId, boolean incrementView) {
        this.auctionId = auctionId;
        this.incrementView = incrementView;
    }
}
