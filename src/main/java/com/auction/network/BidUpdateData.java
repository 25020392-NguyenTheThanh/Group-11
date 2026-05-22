package com.auction.network;

import java.io.Serializable;
// đóng gói dữ liệu bid realtime
// gửi từ server → client qua socket
public class BidUpdateData implements Serializable {
    private static final long serialVersionUID = 1L ; // kiểm tra version object khi serialize

    public int auctionId ;
    public double newHighestBid ;
    public String winnerUsername ;
    public int totalBids ; // tổng số lượt bid

    public BidUpdateData(int auctionId , double newHighestBid , String winnerUsername , int totalBids){
        this.auctionId = auctionId ;
        this.newHighestBid = newHighestBid ;
        this.winnerUsername = winnerUsername ;
        this.totalBids = totalBids ;
    }
}
