package com.auction.model.auction;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction {
    private String id ; // id của phiên đấu giá
    private Item item ; // sản phẩm đang đấu giá
    private double currentHighestBid ; // giá cao nhất hiện tại
    private Bidder currentWinner ; // người đang dẫn đầu
    private AuctionStatus status ;
    private LocalDateTime endTime ;
    private List<BidTransaction> bidHistory ; // danh sách các lần đặt giá

    public Auction(String id , Item item , LocalDateTime endTime){
        this.id = id ;
        this.item = item ;
        this.currentHighestBid = item.getStartingPrice() ; // giá khởi điểm
        this.status = AuctionStatus.OPEN ;
        this.endTime = endTime ;
        this.bidHistory = new ArrayList<>();
    }
    // xử lý các lần đặt giá
    public synchronized void placeBid(Bidder bidder , double amount) throws InvalidBidException , AuctionClosedException {

        // kiểm tra phiên có đang chạy hay không
        if (status != AuctionStatus.RUNNING){
            throw new AuctionClosedException("Phiên đang không tồn tại");
        }

        // kiểm tra giá
        if (amount <= currentHighestBid){
            throw new InvalidBidException("Giá phải cao hơn " + currentHighestBid);
        }

        currentHighestBid = amount ;
        currentWinner = bidder ;

        bidHistory.add((new BidTransaction(bidder , amount)));
    }
    public String getId() { return id; }
    public Item getItem() { return item; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public Bidder getCurrentWinner() { return currentWinner; }
    public AuctionStatus getStatus() { return status; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }

    public void start(){ this.status = AuctionStatus.RUNNING ; }
    public void finish() { this.status = AuctionStatus.FINISHED ; }
}
